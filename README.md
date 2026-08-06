# notiflow — Realtime Order & Notification Platform

한정 수량 상품에 대량 주문이 몰릴 때 발생하는 **재고 동시성 문제**를 재현하고, 서로 다른 동시성 제어 전략(락 없음 / DB 락 / 분산락)을 실측 비교하기 위해 만든 이벤트 기반 MSA 백엔드입니다. 처리 결과는 Kafka를 거쳐 WebSocket으로 실시간 전달됩니다.

- 프론트엔드: [portfolio-front](#) (별도 레포)
- 배포: 백엔드 4개 서비스 + Postgres/Redis/Kafka는 Oracle Cloud, 프론트는 Vercel

---

## 주요 기능

- **동시성 제어 비교**: 락 없음(NONE) / DB Pessimistic Lock / Redisson 분산락(DISTRIBUTED) 3가지 전략으로 같은 시나리오를 재현, 오버셀 발생 여부를 직접 비교
- **오버셀 사후 취소**: 락 없음 상태로 오버셀이 발생하면, 상품별 최근 성공 주문부터 FIFO로 자동 취소(재고 복구) — 실무의 사후 보상 처리와 동일한 원리
- **결제 시뮬레이션**: 주문 성공 후 구매자별 랜덤 지연(0.3~8초)을 두고 결제 확정 처리 (재고 차감 로직과는 분리되어 동시성 벤치마크에 영향 없음)
- **이벤트 기반 실시간 알림**: 주문/결제확정/오버셀 이벤트가 Kafka → notify-service → WebSocket(STOMP)을 거쳐 실시간 브로드캐스트
- JWT 기반 인증 (user-auth-service 발급, 각 서비스가 동일 secret으로 검증)

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

### 서비스 구성

| 서비스 | 포트 | 책임 |
|---|---|---|
| user-auth-service | 8081 | 로그인/로그아웃, Refresh Token(Redis) |
| order-service | 8082 | 주문 생성, 재고 차감/동시성 제어, 오버셀 사후 취소, 결제 시뮬레이션 |
| notify-service | 8083 | 이벤트 소비, 알림 이력 저장/재발행 (내부 전용, 외부 미노출) |
| realtime-gateway-service | 8084 | WebSocket(STOMP) 연결 관리, 실시간 브로드캐스트 |

`common` 모듈(JWT 발급/검증, 공통 응답 포맷, 예외 핸들러, Kafka 토픽 상수)을 각 서비스가 공유합니다.

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

## 핵심 설계 결정

### 1. 왜 락을 3가지 방식으로 비교했는가
"최종적으로 재고가 정확히 줄어드는가"는 세 전략 모두 같지만, 락을 **어디서**(DB vs 애플리케이션 레이어) 잡는지가 다릅니다. 락 없음으로 오버셀을 먼저 재현한 뒤, DB Pessimistic Lock(`SELECT ... FOR UPDATE`)과 Redisson 분산락을 같은 시나리오에 적용해 비교했습니다. 세 전략 모두 동일한 인위적 지연(40ms)을 거치게 해서, "락 때문에 느려 보이는 착시"가 아니라 락 방식 자체의 차이를 공정하게 비교할 수 있게 했습니다.

### 2. AFTER_COMMIT 이벤트 발행으로 dual-write 문제 완화
`OrderService`가 트랜잭션 안에서 곧바로 `KafkaTemplate`을 호출하면, DB가 롤백되더라도 이미 나간 이벤트는 취소할 수 없는 dual-write 문제가 생깁니다. 그래서 `ApplicationEventPublisher`로 이벤트를 "예약"만 해두고, `@TransactionalEventListener(AFTER_COMMIT)`를 통해 트랜잭션이 실제로 커밋된 뒤에만 Kafka로 발행합니다. (다만 이 방식은 "롤백 시 미발행"은 보장하지만, 커밋 후 발행 자체가 실패하는 경우의 재시도까지는 보장하지 않는 약식 구조입니다 — 완전한 Outbox 패턴은 별도 아웃박스 테이블과 폴러가 필요합니다.)

### 3. Self-invocation 문제와 별도 빈 분리
Redisson 분산락(`DistributedLockService.executeWithLock`)과 결제 확정 타이머(`PaymentConfirmationService`)를 각각 별도 Spring 빈으로 분리했습니다. 같은 빈 안에서 `this.method()`처럼 자기 자신의 `@Transactional` 메서드를 호출하면 AOP 프록시를 거치지 않아 트랜잭션이 아예 안 걸리는 self-invocation 문제가 생기기 때문입니다. 컨트롤러/타이머가 항상 다른 빈을 통해 프록시를 거쳐 호출하도록 구조화했습니다.

### 4. 결제 확정을 DB 폴링 대신 인메모리 타이머로
주문 성공 시점에 구매자별 랜덤 지연(0.3~8초)을 이미 알고 있으므로, 별도로 DB를 주기적으로 폴링할 필요 없이 `TaskScheduler`로 그 시간 뒤 딱 한 번 실행될 타이머를 예약합니다. 대신 서비스 재시작 시 예약이 유실되는 트레이드오프가 있습니다.

### 5. 오버셀 사후 취소(보상 처리)
락 없이 오버셀이 발생하면, 상품별로 가장 최근 성공 주문부터 오버셀 수량만큼 자동으로 취소(주문/결제 상태 CANCELLED, 재고 복구)합니다. "누가 오버셀의 원인인지"는 알 수 없기 때문에, 실무에서 오버셀 발견 시 뒤늦게 확정된 주문부터 취소/환불하는 방식을 그대로 따랐습니다.

### 6. 서비스별 이벤트 클래스를 자체 사본으로 보유
notify-service, realtime-gateway-service는 order-service의 이벤트 클래스를 공유하지 않고 필드 구조만 동일한 자체 클래스를 따로 둡니다. 클래스를 공유하면 한쪽이 필드를 바꿀 때 다른 쪽이 컴파일 타임에 안 깨지고 런타임에 조용히 역직렬화 실패하는 결합이 생기기 때문입니다.

### 7. DB는 완전히 분리하지 않고 서비스별 스키마/계정으로 구분
제한된 인프라 자원 안에서, 물리적으로는 하나의 Postgres 서버를 쓰되 서비스별로 DB(user_db/order_db/notify_db)를 분리했습니다.

---

## 아직 없는 것 / 향후 방향

- **개인별(유저 타겟) 알림**: 현재는 `/topic/notifications` 전체 브로드캐스트만 구현되어 있습니다. 유저별 타겟 알림은 STOMP CONNECT 시 JWT로 Principal을 심고 `convertAndSendToUser`를 쓰면 되는데, 아직 미구현입니다.
- **Jenkins CI/CD**: 미구축, 현재는 수동 배포
- **Optimistic Lock**: 비교 대상에 포함하지 않음 (NONE/PESSIMISTIC/DISTRIBUTED 3종만 구현)
- **API Gateway**: 미도입, Caddy가 서브도메인 기준으로 각 서비스에 직접 라우팅

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

API 문서: 각 서비스 기동 후 `http://localhost:{port}/swagger-ui/index.html`

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

- **Self-invocation으로 트랜잭션 미적용**: 분산락/결제확정 타이머를 같은 빈 내부 호출로 구현했다가 트랜잭션이 안 걸리는 걸 발견 → 별도 빈으로 분리해 항상 프록시를 거치도록 수정
- **로그인 2~3초 지연**: BCrypt 기본 강도(10)가 CPU 1코어 배포 환경에 부하로 작용 → 강도를 8로 낮춤 (기존 계정 해시는 그대로 유효)
- **JVM 콜드스타트**: 1코어 환경에서 서비스 하나 뜨는 데 최대 4~5분(282초 실측) — 재시작마다 예열 시간 필요
- **CORS 설정 미반영**: `.env` 수정 후 `docker compose up -d`가 컨테이너를 재생성하지 않고 재시작만 해서 새 값이 반영 안 됨 → `--force-recreate` 필요
- **Oracle 방화벽**: Security List만으론 부족, OCI 이미지 자체 iptables가 22 외 전부 차단하고 있어 80/443을 서버 내부에서도 별도로 열어야 했음

---

## 배포

- Backend: Oracle Cloud (`NOTI-FLOW-APP-SERVER`, `NOTI-FLOW-DB-SERVER`, E2.1.Micro 2대), Caddy + sslip.io로 HTTPS
- Frontend: Vercel
