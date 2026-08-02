package com.seokwon.notiflow.notify.consumer;

import java.util.stream.Collectors;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.seokwon.notiflow.common.kafka.KafkaTopics;
import com.seokwon.notiflow.notify.entity.NotificationEntity;
import com.seokwon.notiflow.notify.event.LockStrategy;
import com.seokwon.notiflow.notify.event.NotificationPublishedEvent;
import com.seokwon.notiflow.notify.event.StockIntegrityEvent;
import com.seokwon.notiflow.notify.repository.NotificationRepository;

/**
 * stock-integrity-events 토픽을 구독해서 배치 재고 정합성 결과를 알림으로 변환/저장하고,
 * OrderEventConsumer와 동일하게 저장 후 notification-events 토픽으로 재발행한다.
 * @description 이 토픽은 order-events와 페이로드 타입이 다르기 때문에(OrderPlacedEvent vs
 * StockIntegrityEvent), 컨슈머 팩토리 공용 설정(spring.json.value.default.type=OrderPlacedEvent)을
 * 그대로 쓰면 이 이벤트를 OrderPlacedEvent로 잘못 역직렬화하게 된다. properties 속성으로
 * 이 리스너에서만 default.type을 오버라이드해서 격리함.
 * 주문 건과 달리 특정 주문(orderId)이나 구매자(buyerUserNo)에 속하지 않는 "배치 요약" 알림이라
 * NOT NULL 컬럼에는 0을 sentinel 값으로 채운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockIntegrityEventConsumer {

    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = KafkaTopics.STOCK_INTEGRITY_EVENTS,
            properties = "spring.json.value.default.type=com.seokwon.notiflow.notify.event.StockIntegrityEvent"
    )
    public void onStockIntegrityEvent(StockIntegrityEvent event) {
        String message = toMessage(event);
        boolean isOversell = event.lostUnits() != null && event.lostUnits() > 0;

        NotificationEntity notification = NotificationEntity.builder()
                .orderId(0L)
                .buyerUserNo(0L)
                .priority(isOversell)
                .category("재고")
                .message(message)
                .isRead(false)
                .createdAt(event.occurredAt())
                .build();

        notificationRepository.save(notification);

        log.debug("재고 정합성 알림 저장: lostUnits={}, lockStrategy={}", event.lostUnits(), event.lockStrategy());

        NotificationPublishedEvent published = new NotificationPublishedEvent(
                notification.getNotificationId(),
                notification.getOrderId(),
                notification.getBuyerUserNo(),
                notification.isPriority(),
                notification.getCategory(),
                notification.getMessage(),
                notification.getCreatedAt()
        );
        kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, String.valueOf(notification.getNotificationId()), published)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("재고 정합성 알림 실시간 발행 실패: notificationId={}", notification.getNotificationId(), ex);
                    }
                });
    }

    private String toMessage(StockIntegrityEvent event) {
        String lockLabel = switch (event.lockStrategy() == null ? LockStrategy.NONE : event.lockStrategy()) {
            case NONE -> "락 없음";
            case PESSIMISTIC -> "DB 락";
            case DISTRIBUTED -> "분산락";
        };
        if (event.lostUnits() != null && event.lostUnits() > 0) {
            String base = "오버셀 %d개 발생 (%s) - 예상 %d개 / 실제 %d개".formatted(
                    event.lostUnits(), lockLabel, event.expectedTotal(), event.actualTotal());
            if (event.oversoldProducts() != null && !event.oversoldProducts().isEmpty()) {
                // 상품별 취소 건수까지 같이 보여줘서, 이 알림만 보고도 어디서 몇 건이 취소됐는지 알 수 있게 함
                String detail = event.oversoldProducts().stream()
                        .map(p -> "%d(%d개)".formatted(p.detailId(), p.lostUnits()))
                        .collect(Collectors.joining(", "));
                base += " / 제품ID: " + detail + " → 해당 건 자동 취소 처리";
            }
            return base;
        }
        return "재고 정합성 일치 (%s) - 예상 %d개 / 실제 %d개".formatted(
                lockLabel, event.expectedTotal(), event.actualTotal());
    }
}
