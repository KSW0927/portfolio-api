package com.seokwon.notiflow.notify.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.seokwon.notiflow.common.kafka.KafkaTopics;
import com.seokwon.notiflow.notify.NotifyEntity;
import com.seokwon.notiflow.notify.event.NotifyPublishedEvent;
import com.seokwon.notiflow.notify.event.PaymentConfirmedEvent;
import com.seokwon.notiflow.notify.NotifyRepository;

/**
 * payment-events 토픽을 구독해서 결제 확정 이벤트를 알림으로 변환/저장하고,
 * OrderEventConsumer와 동일하게 저장 후 notification-events 토픽으로 재발행한다.
 * 이 토픽도 order-events와 페이로드 타입이 다르므로(OrderPlacedEvent vs
 * PaymentConfirmedEvent), StockIntegrityEventConsumer와 동일하게 리스너 단위로
 * spring.json.value.default.type을 오버라이드해서 격리함.
 * 결제완료는 품절/오버셀처럼 최상단에 고정 노출할 필요는 없는 일반 알림이라 priority=false로 저장.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotifyRepository notifyRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_EVENTS,
            properties = "spring.json.value.default.type=com.seokwon.notiflow.notify.event.PaymentConfirmedEvent"
    )
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        String message = "%s %s/%s 결제가 확정되었습니다.".formatted(event.model(), event.storage(), event.color());

        NotifyEntity notify = NotifyEntity.builder()
                .orderId(event.orderId())
                .buyerUserNo(event.buyerUserNo())
                .priority(false)
                .category("결제")
                .message(message)
                .isRead(false)
                .createdAt(event.confirmedAt())
                .build();

        notifyRepository.save(notify);

        log.debug("결제 확정 알림 저장: orderId={}", event.orderId());

        NotifyPublishedEvent published = new NotifyPublishedEvent(
                notify.getNotifyId(),
                notify.getOrderId(),
                notify.getBuyerUserNo(),
                notify.isPriority(),
                notify.getCategory(),
                notify.getMessage(),
                notify.getCreatedAt()
        );
        kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, String.valueOf(notify.getNotifyId()), published)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("결제 확정 알림 실시간 발행 실패: notifyId={}", notify.getNotifyId(), ex);
                    }
                });
    }
}
