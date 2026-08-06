# notiflow — Realtime Order & Notification Platform

한정 수량 상품에 대량 주문이 몰릴 때 발생하는 **재고 동시성 문제**를 재현하고, 서로 다른 동시성 제어 전략(락 없음 / DB 락 / 분산락)을 실측 비교하기 위해 만든 이벤트 기반 MSA 백엔드입니다. 처리 결과는 Kafka를 거쳐 WebSocket으로 실시간 전달됩니다.

**🔗 [라이브 데모](https://portfolio-front-roan-one.vercel.app) *(Ctrl/Cmd+클릭으로 새 탭에서 열기)*

- 프론트엔드 저장소: [portfolio-front](https://github.com/KSW0927/portfolio-front)
- API 문서: [order-service Swagger](https://order.168-107-38-121.sslip.io/swagger-ui/index.html) · [user-auth-service Swagger](https://auth.168-107-38-121.sslip.io/swagger-ui/index.html)
- 배포: 백엔드 4개 서비스 + Postgres/Redis/Kafka는 Oracle Cloud, 프론트는 Vercel

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
OrderEventPublisher → Kafka (order-events / payment-events / stock-integrity-events)
        │
        ▼
notify-service
  - 이벤트 소비 → 알림 문구 생성 → DB 저장(notify_db)
  - Kafka(notification-events)로 재발행
        │
        ▼
realtime-gateway-service
  - notification-events 구독 → STOMP "/topic/notifications" 브로드캐스트
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

세 전략 모두 동일한 인위적 지연(40ms)을 거치게 해서, "락 때문에 느려 보이는 착시"가 아니라 락 방식 자체의 차이를 비교합니다.

| 전략 | 락이 걸리는 곳 | 대기 중 DB 커넥션 점유 |
|---|---|---|
| PESSIMISTIC | DB | O — 대기자가 커넥션 풀을 계속 점유 |
| DISTRIBUTED(Redisson) | Redis | X — 락 대기는 DB 밖에서 일어남 |
| NONE | 없음 | 동시 요청 시 lost-update(오버셀) 재현용 |

### 2. AFTER_COMMIT 이벤트 발행 — dual-write 방지

트랜잭션 안에서 곧바로 `KafkaTemplate`을 호출하면, DB가 롤백되더라도 이미 나간 이벤트는 취소할 수 없습니다. 그래서 이벤트를 `ApplicationEventPublisher`로 "예약"만 해두고, 트랜잭션이 실제로 커밋된 뒤에만 발행합니다.

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

> 이 방식은 "롤백 시 미발행"은 보장하지만, 커밋 후 발행 자체가 실패하는 경우의 재시도까지는 보장하지 않는 약식 구조입니다. 완전한 Outbox 패턴은 별도 아웃박스 테이블과 폴러가 필요합니다.

### 3. 오버셀 사후 취소 (보상 처리)

락 없이 오버셀이 발생하면, 상품별로 가장 최근 성공 주문부터 오버셀 수량만큼 자동 취소합니다. "누가 오버셀의 원인인지"는 알 수 없기 때문에, 실무에서 오버셀 발견 시 뒤늦게 확정된 주문부터 취소/환불하는 방식을 그대로 따랐습니다.

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

### 4. 결제 시뮬레이션 — self-invocation 문제 회피

주문 성공 시 구매자별 랜덤 지연(0.3~8초) 후 결제를 확정합니다. 재고 차감 로직과는 분리되어 있어 동시성 벤치마크에 영향을 주지 않습니다. 결제 확정 로직을 `OrderService`가 아니라 별도 빈으로 분리했는데, 이유는 Spring AOP의 self-invocation 문제 때문입니다.

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

DB 폴링 대신 `TaskScheduler`로 1회성 타이머를 예약하는 방식을 썼습니다 — 주문 시점에 정확한 지연시간을 이미 알고 있어 폴링이 불필요하기 때문입니다. 대신 서비스 재시작 시 예약이 유실되는 트레이드오프가 있습니다.

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

> CI/CD(Jenkins)는 아직 미구축 상태입니다 (남은 과제).

---

## 성능 테스트 결과

> k6 부하테스트 결과로 채울 예정

| 시나리오 | 성공 | 실패(품절) | 최종 재고 | 비고 |
|---|---|---|---|---|
| 락 없음 | - | - | - | 오버셀 발생 여부 |
| Pessimistic Lock | - | - | - | |
| Redis 분산락 | - | - | - | |

---

## 트러블슈팅

- **Self-invocation으로 트랜잭션 미적용**: 분산락/결제확정 타이머를 같은 빈 내부 호출로 구현했다가 트랜잭션이 안 걸리는 걸 발견 → 별도 빈으로 분리
- **로그인 2~3초 지연**: BCrypt 기본 강도(10)가 CPU 1코어 배포 환경에 부하로 작용 → 강도를 8로 낮춤
- **JVM 콜드스타트**: 1코어 환경에서 서비스 하나 뜨는 데 최대 4~5분(282초 실측) — 재시작마다 예열 필요
- **CORS 설정 미반영**: `.env` 수정 후 `docker compose up -d`가 재생성 없이 재시작만 해서 새 값 미반영 → `--force-recreate` 필요
- **Oracle 방화벽**: Security List만으론 부족, OCI 이미지 자체 iptables가 22 외 전부 차단 중이던 것도 발견 → 80/443 서버 내부에서도 별도로 열어야 했음

---

## 아직 없는 것 / 향후 방향

- **개인별(유저 타겟) 알림**: 현재는 `/topic/notifications` 전체 브로드캐스트만 구현. `convertAndSendToUser` 기반 개인 알림은 검토 중
- **Jenkins CI/CD**: 미구축, 현재는 수동 배포

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
