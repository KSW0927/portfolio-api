package com.seokwon.notiflow.gateway.event;

import java.time.LocalDateTime;

/**
 * notify-service가 발행하는 "표시용" 알림 이벤트의 자체 사본
 * @description notify-service/event/NotifyPublishedEvent와 필드가 동일해야 하는 계약.
 * 서비스 간 클래스 공유 없이 각자 사본을 두는 이유는 order-events 쪽과 동일(느슨한 결합).
 */
public record NotifyPublishedEvent(
        Long notifyId,
        Long orderId,
        Long buyerUserNo,
        boolean priority,
        String category,
        String message,
        LocalDateTime createdAt
) {
}
