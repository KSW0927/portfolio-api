# Realtime Order & Notification Platform

한정 수량 상품에 대량 주문이 몰릴 때 발생하는 **재고 동시성 문제**를 해결하고, 처리 결과를 **실시간으로 사용자에게 알리는** 백엔드 시스템입니다. 단일 락 구현을 넘어, 락 전략별 트레이드오프를 실측 비교하고 MSA 환경에서의 트랜잭션·통신 패턴을 실험하는 것을 목표로 합니다.

---

## 주요 기능

- **동시성 제어 비교**: 락 없음 / Pessimistic Lock / Optimistic Lock / Redis 분산락(Redisson) 4가지 모드를 실시간으로 전환하며 오버셀 발생 여부와 처리 성능을 비교
- **실시간 처리 현황 대시보드**: N건 동시 주문 요청 → 처리 결과가 WebSocket으로 실시간 반영되는 그리드 UI
- **이벤트 기반 알림 발송**: 주문 처리 결과를 Kafka 이벤트로 전파, 이메일 및 인앱 실시간 알림으로 발송
- **발송 이력 및 통계 조회**

---

## 아키텍처

```
Client
  │  HTTPS
  ▼
Nginx (리버스 프록시, TLS 종료)
  │
  ├─ REST ──▶ API Gateway ──▶ Order Service / User Service
  │
  └─ WebSocket ──▶ Realtime Gateway Service

────────── 내부 이벤트 흐름 ──────────

Order Service
  - 재고 차감 (락 모드에 따라 동시성 제어)
  - 주문 결과 DB 저장 + Outbox 테이블 기록
        │
        ▼
     Kafka (order.events)
        │
        ▼
Notification Service
  - 이벤트 소비, 발송 이력 저장
  - Redis Pub/Sub으로 처리 결과 publish
        │
        ▼
Realtime Gateway Service
  - Redis Pub/Sub 구독 → 해당 클라이언트에 WebSocket 전달
```

### 서비스 구성

| 서비스 | 책임 |
|---|---|
| Order Service | 주문 생성, 재고 차감/동시성 제어 |
| Notification Service | 이벤트 소비, 채널 라우팅, 발송 이력 관리 |
| Realtime Gateway Service | WebSocket 연결 관리, 실시간 푸시 |
| User Service | 인증, 유저/알림 설정 관리 |

---

## 기술 스택

| 분류 | 사용 기술 |
|---|---|
| Language / Framework | Java, Spring Boot |
| 통신 | REST, WebSocket |
| 메시징 | Kafka |
| 캐시 / 분산락 | Redis, Redisson |
| 영속성 | JPA (Order Service), MyBatis (Notification Service 조회/통계) |
| 인프라 | Docker, Oracle Cloud (ARM), Nginx, Jenkins |
| 부하테스트 | k6 |

---

## 핵심 설계 결정

### 1. 왜 락을 4가지 방식으로 비교했는가
동시성 제어는 "정답 하나"가 아니라 트래픽 패턴에 따른 트레이드오프 문제입니다. 락 없이 발생하는 오버셀 현상을 먼저 재현한 뒤, DB 락 두 종류와 분산락을 같은 시나리오에 적용해 정확성·응답시간·처리량 차이를 직접 비교했습니다.

### 2. 왜 Outbox 패턴을 도입했는가
"DB 커밋"과 "Kafka 이벤트 발행"을 별개 작업으로 처리하면 두 작업 사이의 실패로 데이터 정합성이 깨지는 dual write 문제가 발생합니다. 이벤트를 로컬 트랜잭션 안에서 Outbox 테이블에 먼저 기록하고, 별도 프로세스가 이를 읽어 발행하는 방식으로 원자성을 보장했습니다.

### 3. 왜 Redis를 여러 역할로 사용했는가
Pub/Sub(웹소켓 브로드캐스트), 커넥션 레지스트리, 멱등성 체크(Kafka 중복 소비 방지), Rate Limit, 분산락까지 — 하나의 인메모리 스토어가 서로 다른 문제를 해결하는 데 어떻게 쓰이는지 보여주고자 했습니다.

### 4. 왜 JPA와 MyBatis를 함께 사용했는가
트랜잭션/락 제어가 핵심인 도메인(Order Service)은 JPA의 `@Version`, `@Lock` 등 표준화된 기능을 활용하고, 복잡한 조회·통계가 중심인 도메인(Notification Service)은 MyBatis로 쿼리를 직접 제어하는 방식을 택했습니다.

### 5. 왜 서비스별 DB를 완전히 분리하지 않았는가
제한된 인프라 자원 안에서, 물리적으로는 하나의 DB 컨테이너를 사용하되 서비스별로 스키마와 접근 계정을 분리했습니다. 이를 통해 자원을 아끼면서도 서비스 간 데이터 접근 경계를 강제할 수 있었습니다.

---

## 실행 방법

```bash
git clone <repo-url>
cd <repo-name>

# 전체 스택 실행 (Kafka, Redis, DB, 서비스 전부)
docker-compose up -d

# 개별 서비스 로컬 실행 시
./gradlew :order-service:bootRun
```

> 환경 변수 및 상세 설정은 `docs/setup.md` 참고 (작성 예정)

---

## 성능 테스트 결과

> 구현 완료 후 k6 부하테스트 결과로 채울 예정

| 시나리오 | 성공 | 실패(품절) | 최종 재고 | 비고 |
|---|---|---|---|---|
| 락 없음 | - | - | - | 오버셀 발생 여부 |
| Pessimistic Lock | - | - | - | |
| Optimistic Lock | - | - | - | 재시도 횟수 포함 |
| Redis 분산락 | - | - | - | |

---

## 트러블슈팅

> 개발 중 겪은 문제와 해결 과정을 진행하면서 기록

- (예시) Kafka consumer lag 발생 원인 분석 및 해결
- (예시) ARM64 환경에서의 Docker 이미지 빌드 이슈

---

## 향후 개선 방향

- API Gateway 도입 검토
- Saga 패턴 적용 (Order/Coupon 서비스 완전 분리 시)
- gRPC ↔ REST 성능 비교 (내부 서비스 간 통신 구간)
