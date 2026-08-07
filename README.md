# NOTI-FLOW — 재고 동시성 제어 비교 & 실시간 알림 데모

한정 수량 상품에 대량 주문이 몰릴 때 발생하는 **재고 동시성 문제**를 재현하고, 서로 다른 동시성 제어 전략(락 없음 / DB 락 / 분산락)을 실측 비교하기 위해 만든 이벤트 기반 MSA 백엔드입니다. 처리 결과는 Kafka를 거쳐 WebSocket으로 실시간 전달됩니다.

**🔗 [라이브 데모](https://portfolio-front-roan-one.vercel.app)** *(Ctrl/Cmd+클릭으로 새 탭에서 열기)*

- 프론트엔드 저장소: [portfolio-front](https://github.com/KSW0927/portfolio-front)
- API 문서: [order-service Swagger](https://order.168-107-38-121.sslip.io/swagger-ui/index.html) · [user-auth-service Swagger](https://auth.168-107-38-121.sslip.io/swagger-ui/index.html)
- 배포: 백엔드 4개 서비스 + Postgres/Redis/Kafka는 Oracle Cloud, 프론트는 Vercel

**한눈에 보기**
- 동시성 제어 3가지(락 없음 / DB 락 / 분산락)를 같은 시나리오로 실측 비교
- 트랜잭션 커밋 후에만 이벤트 발행(dual-write 방지) > Kafka > WebSocket 실시간 알림
- 오버셀 발생 시 사후 자동 취소(보상 처리), 결제 확정 시뮬레이션
- 단위테스트 26개, 트러블슈팅 5건 기록

---

## 아키텍처

```
Client (Vercel, HTTPS)
        │
        ▼
Caddy (Let's Encrypt 자동 인증서, 서브도메인별 라우팅)
        │
   ┌────┼──────────────┬───────────────┐
   ▼    ▼              ▼               ▼
 auth  order          notify           gw
 :8081 :8082          :8083            :8084 (WebSocket)

────────── 내부 이벤트 흐름 ──────────

order-service
  - 재고 차감 (LockStrategy: NONE / PESSIMISTIC / DISTRIBUTED)
  - 주문 저장 + ApplicationEventPublisher로 이벤트 "예약"
        │
        ▼ (트랜잭션 AFTER_COMMIT 시점에만)
OrderEventPublisher > Kafka (order-events / payment-events / stock-integrity-events)
        │
        ▼
notify-service
  - 이벤트 소비 > 알림 문구 생성 > DB 저장(notify_db)
  - Kafka(notification-events)로 재발행
        │
        ▼
realtime-gateway-service
  - notification-events 구독 > STOMP "/topic/notifications" 브로드캐스트
```

| 서비스 | 포트 | 책임 |
|---|---|---|
| user-auth-service | 8081 | 로그인/로그아웃, Refresh Token(Redis) |
| order-service | 8082 | 주문 생성, 재고 차감/동시성 제어, 오버셀 사후 취소, 결제 시뮬레이션 |
| notify-service | 8083 | 이벤트 소비, 알림 이력 저장/재발행 (내부 전용, 외부 미노출) |
| realtime-gateway-service | 8084 | WebSocket(STOMP) 연결 관리, 실시간 브로드캐스트 |

`common` 모듈(JWT 발급/검증, 공통 응답 포맷, 예외 핸들러, Kafka 토픽 상수)을 각 서비스가 공유합니다.

---

## 핵심 기능

### 1. 동시성 제어 3종 비교

같은 재고 차감 로직을, 락을 어디서(DB vs 애플리케이션 레이어) 잡는지만 바꿔가며 비교합니다.

<details>
<summary><strong>코드 보기</strong> — 락 전략별 조회/실행 분기</summary>

```java
// OrderService.placeOrder — PESSIMISTIC만 SELECT ... FOR UPDATE로 조회
ProductDetailEntity detail = (lockStrategy == LockStrategy.PESSIMISTIC
        ? productDetailRepository.findByIdForUpdate(detailId)   // 순차 처리
        : productDetailRepository.findById(detailId))            // 락 없음
        .orElseThrow(...);
```

```java
// OrderController — DISTRIBUTED는 트랜잭션 시작 "전에" 분산락을 먼저 잡아야 해서
// 컨트롤러가 별도 빈(DistributedLockService)으로 감싸서 호출
OrderResultDTO result = lockStrategy == LockStrategy.DISTRIBUTED
        ? distributedLockService.executeWithLock(
                "stock-lock:" + dto.getProductDetailId(),
                () -> orderService.placeOrder(dto.getProductDetailId(), dto.getBuyerUserNo(), lockStrategy))
        : orderService.placeOrder(dto.getProductDetailId(), dto.getBuyerUserNo(), lockStrategy);
```

</details>

세 전략 모두 동일한 인위적 지연(40ms)을 거치게 해서, "락 때문에 느려 보이는 착시"가 아니라 락 방식 자체의 차이를 비교합니다.

| 전략 | 락이 걸리는 곳 | 대기 중 DB 커넥션 점유 |
|---|---|---|
| PESSIMISTIC | DB | O — 대기자가 커넥션 풀을 계속 점유 |
| DISTRIBUTED(Redisson) | Redis | X — 락 대기는 DB 밖에서 일어남 |
| NONE | 없음 | 동시 요청 시 lost-update(오버셀) 재현용 |

### 2. AFTER_COMMIT 이벤트 발행 — dual-write 방지

트랜잭션 안에서 곧바로 `KafkaTemplate`을 호출하면, DB가 롤백되더라도 이미 나간 이벤트는 취소할 수 없습니다.<br>
그래서 이벤트를 `ApplicationEventPublisher`로 "예약"만 해두고, 트랜잭션이 실제로 커밋된 뒤에만 발행합니다.

<details>
<summary><strong>코드 보기</strong> — AFTER_COMMIT 이벤트 발행</summary>

```java
// OrderEventPublisher
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onOrderPlaced(OrderPlacedEvent event) {
    kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, String.valueOf(event.orderId()), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka 이벤트 발행 실패: orderId={}", event.orderId(), ex);
                }
            });
}
```

</details>

> 이 방식은 "롤백 시 미발행"은 보장하지만, 커밋 후 발행 자체가 실패하는 경우의 재시도까지는 보장하지 않는 약식 구조입니다.<br>
완전한 Outbox 패턴은 별도 아웃박스 테이블과 폴러가 필요합니다.

### 3. 오버셀 사후 취소 (보상 처리)

락 없이 오버셀이 발생하면, 상품별로 가장 최근 성공 주문부터 오버셀 수량만큼 자동 취소합니다. "누가 오버셀의 원인인지"는 알 수 없기 때문에<br>
실무에서 오버셀 발견 시 확정된 주문부터 취소/환불하는 방식을 그대로 따랐습니다.

<details>
<summary><strong>코드 보기</strong> — 오버셀 사후 취소</summary>

```java
// OrderService.cancelOversoldOrders
List<OrderEntity> candidates =
    orderRepository.findRecentSuccessOrders(detailId, PageRequest.of(0, cancelCount)); // FIFO

// 재고 복구도 락을 걸고 진행 — 다음 배치가 이미 시작돼서
// 같은 상품에 동시 접근할 가능성을 대비
ProductDetailEntity detail = productDetailRepository.findByIdForUpdate(detailId).orElse(null);

for (OrderEntity order : candidates) {
    order.setStatus(OrderStatus.CANCELLED);
    order.setPaymentStatus(PaymentStatus.CANCELLED);
    if (detail != null) detail.setStock(detail.getStock() + 1);
}
```

</details>

### 4. 결제 시뮬레이션 — self-invocation 문제 회피

주문 성공 시 구매자별 랜덤 지연(0.3~8초) 후 결제를 확정합니다. 재고 차감 로직과는 분리되어 있어 동시성 벤치마크에 영향을 주지 않습니다.<br>
결제 확정 로직을 `OrderService`가 아니라 별도 빈으로 분리했는데 이유는 Spring AOP의 self-invocation 문제 때문입니다.

<details>
<summary><strong>코드 보기</strong> — 결제 확정 서비스 분리</summary>

```java
/**
 * 별도 빈으로 분리한 이유: 스케줄된 람다가 "같은 빈 안의 다른 @Transactional 메서드"를
 * 호출하는 self-invocation 패턴이 되면 프록시가 가로채지 못해 트랜잭션이 아예 안 걸림.
 * 별도 빈으로 두면 항상 프록시를 거쳐 호출되므로 이 문제가 생기지 않는다.
 */
@Service
public class PaymentConfirmationService {
    @Transactional
    public void confirmPayment(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getPaymentStatus() != PaymentStatus.PENDING) return;
        // ... 결제 확정 처리
    }
}
```

</details>

DB 폴링 대신 `TaskScheduler`로 1회성 타이머를 예약하는 방식을 썼습니다 — 주문 시점에 정확한 지연시간을 이미 알고 있어 폴링이 불필요하기 때문입니다.

---

## 기술 스택

| 분류 | 사용 기술 |
|---|---|
| Language / Framework | Java 21, Spring Boot 3.3.7 |
| 통신 | REST, WebSocket(STOMP, SockJS) |
| 메시징 | Kafka (KRaft 단일 브로커) |
| 캐시 / 분산락 | Redis, Redisson |
| 영속성 | Spring Data JPA, PostgreSQL (서비스별 DB 분리: user_db / order_db / notify_db) |
| 인증 | JWT(jjwt), Spring Security |
| API 문서 | springdoc-openapi (Swagger UI) |
| 인프라 | Docker, Docker Compose, Oracle Cloud, Caddy(리버스 프록시 + 자동 HTTPS), sslip.io |
| 빌드 | Gradle 멀티모듈 |

> CI/CD(Jenkins)는 아직 미구축 상태입니다 (2026.08.07 기준).

---

## 테스트

핵심 비즈니스 로직 위주로 Mockito 기반 단위 테스트를 작성했습니다. DB·Kafka·Redis 없이 Repository/이벤트 발행자를 mock 처리해서<br>
위에서 설명한 설계(락 전략 분기, self-invocation 회피, 오버셀 사후 취소)가 실제로 지켜지는지 빠르게 검증합니다.

| 서비스 | 클래스 | 검증 내용 | 개수 |
|---|---|---|---|
| order-service | OrderServiceTest | 락 전략별 조회 분기, 재고 성공/품절 처리, 오버셀 사후 취소 | 10 |
| order-service | DistributedLockServiceTest | 분산락 획득/해제, 예외·인터럽트 상황에서도 락이 반드시 풀리는지 | 5 |
| order-service | PaymentConfirmationServiceTest | PENDING 건만 확정 처리, 취소된 건은 되돌리지 않는지 | 4 |
| notify-service | 컨슈머 테스트 3종 | 이벤트>알림 문구/우선순위/카테고리 변환, Kafka 재발행 | 7 |
| **합계** | | | **26** |

> 인증(로그인/로그아웃)처럼 이 프로젝트만의 특이점이 없는 보일러플레이트 영역은 테스트 우선순위에서 제외했습니다.

**결제 확정 — 오버셀로 취소된 건은 되돌아가지 않는다.** 위 "결제 시뮬레이션" 설계가 실제로 지키는 불변조건: 오버셀 사후 취소로 CANCELLED된 주문을, 뒤늦게 도착한 결제확정 타이머가 다시 COMPLETED로 되돌리면 안 됩니다.

<details>
<summary><strong>코드 보기</strong> — PaymentConfirmationServiceTest</summary>

```java
@Test
void 오버셀로_취소된_주문이면_뒤늦게_도착한_타이머가_결제완료로_되돌리지_않는다() {
    OrderEntity order = OrderEntity.builder()
            .orderId(101L).buyer(buyer).productDetail(detail)
            .status(OrderStatus.CANCELLED).paymentStatus(PaymentStatus.CANCELLED)
            .createdAt(LocalDateTime.now()).build();
    given(orderRepository.findById(101L)).willReturn(Optional.of(order));

    paymentConfirmationService.confirmPayment(101L);

    assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
    verifyNoInteractions(eventPublisher);
}
```

</details>

**분산락 — action 실행 중 예외가 나도 반드시 해제된다.** 락이 안 풀리는 버그는 이후 모든 요청을 막아버리는 심각한 장애로 이어지기 때문에, finally 블록의 방어 로직이 실제로 지켜지는지를 검증합니다.

<details>
<summary><strong>코드 보기</strong> — DistributedLockServiceTest</summary>

```java
@Test
void action_실행_중_예외가_나도_락은_반드시_해제된다() throws InterruptedException {
    given(lock.tryLock(5L, TimeUnit.SECONDS)).willReturn(true);
    given(lock.isHeldByCurrentThread()).willReturn(true);

    Supplier<String> action = () -> { throw new RuntimeException("boom"); };

    assertThatThrownBy(() -> distributedLockService.executeWithLock(LOCK_KEY, action))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");

    verify(lock).unlock();
}
```

</details>

---

## 트러블슈팅

- **Self-invocation으로 트랜잭션 미적용** > 분산락/결제확정 타이머를 별도 빈으로 분리해 항상 프록시를 거치도록 수정
- **로그인 2~3초 지연** > BCrypt 강도를 1코어 배포 환경에 맞게 10>8로 낮춤
- **JVM 콜드스타트(최대 4~5분, 282초 실측)** > 1코어 환경 특성상 재시작마다 예열 시간 감안
- **CORS 설정 미반영** > `.env` 수정 후 `docker compose up -d --force-recreate`로 컨테이너 재생성 필요
- **Oracle 방화벽 이중 차단** > Security List 외에 OCI 이미지 자체 iptables도 80/443 별도로 열어야 함

---

## 실행 방법

```bash
git clone https://github.com/KSW0927/portfolio-api.git
cd portfolio-api
git checkout feature/deploy

# 인프라 (Kafka + Redis)
docker compose up -d

# .env 파일 준비 (.env.example 참고: DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET)
cp .env.example .env

# 서비스별 DB 생성 필요: user_db / order_db / notify_db

# 개별 서비스 실행
./gradlew :user-auth-service:bootRun
./gradlew :order-service:bootRun
./gradlew :notify-service:bootRun
./gradlew :realtime-gateway-service:bootRun
```
