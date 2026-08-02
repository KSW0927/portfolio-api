# notiflow

Kafka 기반 이벤트 처리와 Pessimistic Lock 동시성 제어를 직접 구현/검증해보기 위해 만든 MSA 포트폴리오 프로젝트입니다.

"동시에 몰리는 주문에서 재고가 왜 깨지는지", "그걸 막으면 무엇이 달라지는지", "처리 결과를 실시간으로 어떻게 전달하는지"를 최소 구성으로 직접 만들어서 눈으로 확인하는 데 초점을 맞췄습니다.

## 데모 시나리오

1. 프론트에서 100/500/1000건 규모의 동시 주문을 시뮬레이션으로 쏩니다.
2. **Pessimistic Lock 적용/미적용**을 토글해서 같은 부하로 두 번 돌려봅니다.
   - 적용: 재고 행에 락을 걸어 순차 처리 → 예상 재고와 실제 재고가 항상 일치
   - 미적용: 락 없이 조회 후 차감 → 동시 요청이 겹치면서 lost update(오버셀) 재현
3. 처리된 주문은 Kafka를 거쳐 알림으로 가공되고, WebSocket(STOMP)으로 화면에 실시간으로 꽂힙니다.

## 아키텍처

```mermaid
flowchart LR
    FE["Frontend (React)"]

    subgraph Services
        AUTH["user-auth-service :8081"]
        ORDER["order-coupon-service :8082"]
        NOTI["notification-service :8083"]
        GW["realtime-gateway-service :8084"]
    end

    REDIS[(Redis)]
    KAFKA{{Kafka}}

    UDB[(user_db)]
    ODB[(order_db)]
    NDB[(notify_db)]

    FE -- "REST: 로그인/회원가입" --> AUTH
    FE -- "REST: 주문/상품조회" --> ORDER
    FE -- "WebSocket(STOMP) 구독" --> GW

    AUTH --> UDB
    AUTH -- "Refresh Token" --> REDIS
    ORDER --> ODB
    NOTI --> NDB

    ORDER -- "order-events 발행\n(주문 처리 후 커밋 시점)" --> KAFKA
    KAFKA -- "order-events 구독" --> NOTI
    NOTI -- "notification-events 발행\n(알림 저장 후)" --> KAFKA
    KAFKA -- "notification-events 구독" --> GW
    GW -- "STOMP /topic/notifications" --> FE
```

같은 JWT(`jwt.secret` 공유)를 order-coupon-service가 그대로 검증하는 방식으로 인증을 최소 구성했고, 서비스 간 직접 호출 대신 Kafka로 결합도를 낮췄습니다. DB는 서비스별로 분리(`user_db` / `order_db` / `notify_db`)해서 데이터 소유권을 서비스 안에 가둬뒀습니다.

## 모듈 구성

Gradle 멀티모듈이며, 서비스 간 공통 코드는 `common`에 모아두고 각 서비스가 이를 의존합니다.

```
notiflow/
├── common/                    # 서비스 전체가 공유하는 코드
├── user-auth-service/         # 회원가입/로그인/로그아웃 (:8081)
├── order-coupon-service/      # 주문 처리 + Pessimistic Lock 데모 (:8082)
├── notification-service/      # Kafka 소비 → 알림 가공/저장 (:8083)
├── realtime-gateway-service/  # Kafka 소비 → WebSocket 브로드캐스트 (:8084)
└── docker-compose.yml         # 로컬 개발용 Kafka + Redis
```

### common — 공유 모듈

| 클래스 | 역할 |
|---|---|
| `security.jwt.JwtTokenProvider` | JWT 발급/검증. `user-auth-service`가 발급하고, `order-coupon-service`는 같은 secret으로 검증만 함(재발급 없음) |
| `security.jwt.JwtAuthenticationFilter` | `Authorization: Bearer` 헤더를 읽어 SecurityContext에 인증 정보를 채우는 필터. 각 서비스의 `SecurityConfig`가 직접 등록 |
| `security.config.CommonSecurityConfig` | 자체 인증 로직이 없는 서비스(notification, gateway)용 기본 Security 설정(permitAll + CORS). 자체 `SecurityConfig`를 갖는 서비스(user-auth, order-coupon)는 컴포넌트 스캔에서 이 클래스를 제외 |
| `response.ApiResponse` / `ResponseResult` | `{ code, message, data }` 형태로 통일한 공통 응답 포맷. 응답 코드/메시지는 Enum(`ResponseResult`)으로 관리 |
| `handler.RespExcpHandler` | `@RestControllerAdvice(basePackages = "com.seokwon.notiflow")`로 전체 서비스의 예외를 한 곳에서 `ApiResponse` 형태로 변환 |
| `exception.BusinessException` / `NotFoundException` | 도메인 예외. `RespExcpHandler`가 이걸 받아 적절한 HTTP 상태로 변환 |
| `kafka.KafkaTopics` | Kafka 토픽 이름 상수(`order-events`, `notification-events`). Producer/Consumer가 문자열을 직접 들고 있지 않고 여기서 공유 |

각 서비스는 `@ComponentScan(basePackages = "com.seokwon.notiflow")`로 `common`의 컴포넌트를 그대로 끌어와 씁니다. 대신 서비스마다 필요 없는 공용 Bean(예: 자체 Security 설정이 있는 서비스에서 `CommonSecurityConfig`)은 `excludeFilters`로 명시적으로 제외합니다.

### user-auth-service — 인증

```
userauth/
├── UserController        POST /api/users/signUp, /login, /logout/{userNo}
├── UserService            회원가입/로그인/로그아웃 비즈니스 로직
├── UserEntity              users 테이블
├── UserRepository
├── config/SecurityConfig   signUp·login만 permitAll, 나머지는 JWT 인증 필요
├── config/RedisConfig      Lettuce 기반 StringRedisTemplate
├── redis/RedisService      Refresh Token 저장/조회/삭제 (key: refresh:{userId})
└── dto/                    LoginRequestDTO, LoginResponseDTO, SignUpRequestDTO, SignUpResponseDTO
```

로그인 성공 시 `JwtTokenProvider`로 Access/Refresh Token을 발급하고, Refresh Token은 DB가 아닌 **Redis**에 저장합니다(세션 조회 병목 회피, 로그아웃 시 즉시 무효화 가능). DB는 `user_db`.

### order-coupon-service — 주문 + 동시성 데모

```
order/
├── OrderController                     GET /api/orders/products, POST /api/orders, POST /api/orders/reset
├── service/OrderService                 주문 처리 + Kafka 이벤트 발행
├── service/ProductService               상품/재고 목록 조회
├── entity/ProductEntity                 상품 모델(예: Galaxy Z Flip8)
├── entity/ProductDetailEntity           실제 판매 단위(SKU) - 용량+색상 조합별 재고. 락 대상
├── entity/CustomerEntity                시뮬레이션용 테스트 구매자 풀(2,000명)
├── entity/OrderEntity                   주문 처리 이력(성공/품절 모두 기록)
├── repository/ProductDetailRepository   findByIdForUpdate (PESSIMISTIC_WRITE)
├── event/OrderPlacedEvent               주문 처리 결과 이벤트(record)
├── event/OrderEventPublisher            @TransactionalEventListener(AFTER_COMMIT)로 Kafka 발행
├── seed/ProductSeeder, TestBuyerSeeder   상품/재고, 테스트 구매자 2,000명 시드
└── config/SecurityConfig                 상품 목록(GET)만 공개, 주문/초기화는 JWT 인증 필요
```

**Pessimistic Lock 스위치** (`OrderService.placeOrder(detailId, buyerUserNo, useLock)`)

```java
ProductDetailEntity detail = (useLock
        ? productDetailRepository.findByIdForUpdate(detailId)   // PESSIMISTIC_WRITE, 순차 처리
        : productDetailRepository.findById(detailId))            // 락 없음, 동시 요청 시 lost update 재현
        .orElseThrow(...);
```

두 경로 모두 동일한 인위적 지연(`Thread.sleep(40ms)`)을 거치게 해서, "락이 있어서 느린 것"이 아니라 "같은 조건에서 락 유무 자체가 결과를 가른다"를 공정하게 비교할 수 있게 했습니다.

**이벤트 발행은 `ApplicationEventPublisher` → `@TransactionalEventListener(AFTER_COMMIT)`을 거칩니다.** `OrderService`가 트랜잭션 안에서 곧바로 `KafkaTemplate`을 호출하면, DB 트랜잭션이 롤백되더라도 이미 Kafka로 나간 메시지는 취소할 수 없는 **dual-write 문제**가 생깁니다. 그래서 이벤트는 트랜잭션 커밋 이후에만 실제로 Kafka에 발행되도록 분리했습니다.

DB는 `order_db`.

### notification-service — 알림 가공/저장

```
notification/
├── consumer/OrderEventConsumer          order-events 구독 → 알림 문구 생성 → DB 저장 → notification-events 발행
├── entity/NotificationEntity            notify 테이블(orderId, category, message, isRead, createdAt)
├── repository/NotificationRepository
└── event/
    ├── OrderPlacedEvent                  order-coupon-service 이벤트의 자체 사본
    ├── OrderStatus                       위와 동일한 이유로 자체 사본
    └── NotificationPublishedEvent        저장 완료된(표시용) 알림 이벤트
```

order-coupon-service의 이벤트 클래스를 그대로 가져다 쓰지 않고, **필드 구조만 동일한 별도 클래스를 자체적으로 보유**합니다. 서비스 간에 클래스를 공유하면 한쪽이 필드를 바꿀 때 다른 쪽이 컴파일 타임에 깨지지 않고 런타임에 조용히 역직렬화 실패하는 결합이 생기기 때문입니다. Kafka 메시지도 `spring.json.add.type.headers=false`로 producer 클래스의 풀패키지명을 헤더에 싣지 않고, consumer 쪽에서 `spring.json.value.default.type`으로 자기 소유 클래스에 고정 매핑합니다.

DB 저장이 끝나면 같은 메서드 안에서 바로 `notification-events`를 발행합니다(`NotificationRepository.save()`가 Spring Data JPA 자체 트랜잭션으로 즉시 커밋되므로, order-coupon-service 때와 달리 별도의 AFTER_COMMIT 처리가 필요 없습니다). DB는 `notify_db`.

### realtime-gateway-service — 실시간 브로드캐스트

```
gateway/
├── config/WebSocketConfig               STOMP 엔드포인트(/ws, SockJS) + 메시지 브로커(/topic)
├── consumer/NotificationEventConsumer   notification-events 구독 → /topic/notifications로 즉시 브로드캐스트
└── event/NotificationPublishedEvent     notification-service 이벤트의 자체 사본
```

가공/판단 로직 없이 받은 그대로 중계만 합니다. 별도 REST 트리거 대신 **Kafka를 직접 구독**하게 해서 notification-service와 REST로 얽히지 않도록 했고, 둘 중 하나가 잠깐 죽어도 Kafka가 메시지를 들고 있다가 재연결 시 이어받습니다.

## 실시간 알림 파이프라인 (전체 흐름)

```
1. 프론트  → POST /api/orders  (order-coupon-service)
2. OrderService.placeOrder()
     - Pessimistic Lock 적용/미적용 분기로 재고 차감
     - OrderEntity 저장
     - ApplicationEventPublisher.publishEvent(OrderPlacedEvent)   ← 트랜잭션 안, 아직 Kafka로 안 나감
3. 트랜잭션 커밋 완료
     - OrderEventPublisher(@TransactionalEventListener AFTER_COMMIT)가 그제서야 Kafka "order-events"로 발행
4. notification-service: OrderEventConsumer가 order-events 구독
     - 상태(성공/품절)에 맞는 알림 문구 생성
     - NotificationEntity 저장 (notify_db)
     - Kafka "notification-events"로 재발행
5. realtime-gateway-service: NotificationEventConsumer가 notification-events 구독
     - SimpMessagingTemplate으로 "/topic/notifications" 브로드캐스트
6. 프론트: STOMP 구독 중인 notificationStore가 메시지 수신 → 알림 위젯에 실시간 반영
```

## 로컬 실행

### 1. 인프라 (Kafka + Redis)

```bash
cd backend
docker compose up -d
docker compose ps
```

### 2. 데이터베이스

서비스별로 DB를 분리해서 씁니다. 아직 없다면 먼저 생성합니다.

```sql
CREATE DATABASE user_db;
CREATE DATABASE order_db;
CREATE DATABASE notify_db;
```

각 서비스는 `DB_URL` 환경변수로 자신의 DB를 가리키게 되어 있습니다(설정 안 하면 기본값 `devdb`로 떨어짐).

| 서비스 | 환경변수 예시 |
|---|---|
| user-auth-service | `DB_URL=jdbc:postgresql://<host>:5432/user_db` |
| order-coupon-service | `DB_URL=jdbc:postgresql://<host>:5432/order_db` |
| notification-service | `DB_URL=jdbc:postgresql://<host>:5432/notify_db` |
| realtime-gateway-service | 엔티티가 없어 필수는 아니지만, datasource 설정 자체는 붙어있어 접속 가능한 DB 지정 필요 |

공통으로 필요한 환경변수: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`(HS256 요구사항상 **32자 이상** 필수 - 짧으면 `WeakKeyException`으로 기동 실패).

### 3. 서비스 기동 순서

Kafka/Redis → user-auth-service → order-coupon-service → notification-service → realtime-gateway-service 순으로 띄우는 걸 권장합니다(주문/알림 파이프라인이 서로를 필요로 하므로).

| 서비스 | 포트 |
|---|---|
| user-auth-service | 8081 |
| order-coupon-service | 8082 |
| notification-service | 8083 |
| realtime-gateway-service | 8084 |

## 기술 스택

**Backend**: Java 21, Spring Boot 3.3.7, Spring Data JPA, Spring Security, Spring Kafka, Spring WebSocket(STOMP), Redis(Lettuce), PostgreSQL, JWT(jjwt), Gradle 멀티모듈

**Infra(로컬 개발)**: Docker Compose (Kafka - KRaft 단일 브로커, Redis)

**Frontend**: React, TypeScript, Zustand, Axios, @stomp/stompjs + SockJS — 자세한 내용은 [portfolio-front](https://github.com/KSW0927/portfolio-front) 참고
