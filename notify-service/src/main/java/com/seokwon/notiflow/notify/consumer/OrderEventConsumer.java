package com.seokwon.notiflow.notify.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.seokwon.notiflow.common.kafka.KafkaTopics;
import com.seokwon.notiflow.notify.entity.NotificationEntity;
import com.seokwon.notiflow.notify.event.NotificationPublishedEvent;
import com.seokwon.notiflow.notify.event.OrderPlacedEvent;
import com.seokwon.notiflow.notify.event.OrderStatus;
import com.seokwon.notiflow.notify.repository.NotificationRepository;

/**
 * order-events 토픽을 구독해서 주문 이벤트를 알림으로 변환/저장하고, 저장이 끝나면
 * notification-events 토픽으로 다시 발행해서 realtime-gateway-service가 실시간으로 화면에 뿌리게 한다.
 * @description groupId는 application.properties의 spring.kafka.consumer.group-id를 그대로 씀
 * (notify-service 인스턴스가 여러 개여도 같은 group이면 파티션을 나눠 가지므로 중복 소비 안 됨).
 * repository.save()는 Spring Data JPA가 자체 트랜잭션으로 즉시 커밋하므로(이 메서드 자체는
 * @Transactional이 아님), save 이후 바로 Kafka로 보내도 order-service 때와 같은
 * dual-write 문제가 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS)
    public void onOrderEvent(OrderPlacedEvent event) {
        String message = toMessage(event);

        // 품절/취소 둘 다 구매자가 바로 확인해야 하는 중요 알림이라 최상단 고정(priority) 대상
        boolean priority = event.status() == OrderStatus.OUT_OF_STOCK || event.status() == OrderStatus.CANCELLED;
        String category = event.status() == OrderStatus.CANCELLED ? "결제취소" : "주문";

        NotificationEntity notification = NotificationEntity.builder()
                .orderId(event.orderId())
                .buyerUserNo(event.buyerUserNo())
                .priority(priority)
                .category(category)
                .message(message)
                .isRead(false)
                .createdAt(event.occurredAt())
                .build();

        notificationRepository.save(notification);

        log.debug("주문 이벤트 알림 저장: orderId={}, status={}", event.orderId(), event.status());

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
                        log.error("알림 실시간 발행 실패: notificationId={}", notification.getNotificationId(), ex);
                    }
                });
    }

    private String toMessage(OrderPlacedEvent event) {
        String item = "%s %s/%s".formatted(event.model(), event.storage(), event.color());
        return switch (event.status()) {
            case SUCCESS -> item + " 주문이 접수되었습니다.";
            case OUT_OF_STOCK -> item + " 주문이 품절로 실패했습니다.";
            case CANCELLED -> item + " 주문이 재고 부족(오버셀)으로 취소되었습니다. 환불이 진행됩니다.";
        };
    }
}
