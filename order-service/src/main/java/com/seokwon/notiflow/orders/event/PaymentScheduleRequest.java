package com.seokwon.notiflow.orders.event;

/**
 * 결제 확정 타이머 예약 요청 - Kafka로 나가지 않는 순수 인프로세스 Spring 이벤트
 * @description placeOrder 트랜잭션이 커밋된 뒤(AFTER_COMMIT)에만 타이머를 예약해야
 * "아직 커밋도 안 된 주문을 확정 처리하려는" 레이스를 피할 수 있어서, 다른 Kafka 발행 이벤트들과
 * 동일하게 OrderEventPublisher가 @TransactionalEventListener로 받아 처리한다.
 */
public record PaymentScheduleRequest(
        Long orderId,
        long delayMs
) {
}
