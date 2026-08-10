package com.seokwon.notiflow.orders.event;

import java.time.Instant;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.seokwon.notiflow.common.kafka.KafkaTopics;
import com.seokwon.notiflow.orders.order.PaymentConfirmationService;

/**
 * 주문 이벤트를 실제로 Kafka에 발행하는 리스너
 * OrderService는 DB 트랜잭션 안에서 ApplicationEventPublisher로 이 이벤트를 "예약"만 해두고,
 * 이 리스너가 @TransactionalEventListener(AFTER_COMMIT)로 받아서 트랜잭션이 실제로 커밋된 뒤에만 Kafka로 보낸다.
 * 이렇게 분리하지 않고 트랜잭션 도중에 바로 Kafka로 보내면, DB는 롤백됐는데 이벤트는 이미 나가버리는
 * dual-write 불일치가 생길 수 있음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TaskScheduler paymentConfirmTaskScheduler;
    private final PaymentConfirmationService paymentConfirmationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 이벤트 발행 실패: orderId={}", event.orderId(), ex);
                    } else {
                        log.debug("Kafka 이벤트 발행 성공: orderId={}, topic={}, partition={}, offset={}",
                                event.orderId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("결제 확정 이벤트 발행 실패: orderId={}", event.orderId(), ex);
                    } else {
                        log.debug("결제 확정 이벤트 발행 성공: orderId={}, topic={}, partition={}, offset={}",
                                event.orderId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * 결제 확정 타이머 예약
     * DB 폴링 스케줄러 대신, 주문 커밋 직후 정확한 지연시간만큼 딱 한 번 실행될 타이머를 건다.
     * AFTER_COMMIT에서 예약하는 이유는 order row가 실제로 커밋돼서 다른 트랜잭션에서도 보이는 상태가
     * 된 뒤에만 타이머가 안전하게 그 행을 찾아 처리할 수 있기 때문.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentScheduleRequested(PaymentScheduleRequest event) {
        paymentConfirmTaskScheduler.schedule(
                () -> paymentConfirmationService.confirmPayment(event.orderId()),
                Instant.now().plusMillis(event.delayMs())
        );
    }

    /**
     * 배치 재고 정합성 결과 발행
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockIntegrityReported(StockIntegrityEvent event) {
        kafkaTemplate.send(KafkaTopics.STOCK_INTEGRITY_EVENTS, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("재고 정합성 이벤트 발행 실패", ex);
                    } else {
                        log.debug("재고 정합성 이벤트 발행 성공: lostUnits={}, topic={}, partition={}, offset={}",
                                event.lostUnits(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
