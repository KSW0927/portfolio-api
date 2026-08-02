package com.seokwon.notiflow.notify.event;

import java.time.LocalDateTime;

/**
 * DB 저장이 끝난 알림을 실시간으로 내보내기 위한 이벤트
 * @description OrderPlacedEvent(주문 도메인 원본)와 달리, 이미 사람이 읽을 문구로 가공되고
 * notificationId가 채번된 "표시용" 데이터. realtime-gateway-service가 이 토픽만 구독하면
 * 주문 도메인을 몰라도 그대로 화면에 뿌릴 수 있음.
 */
public record NotificationPublishedEvent(
        Long notificationId,
        Long orderId,
        Long buyerUserNo,
        boolean priority,
        String category,
        String message,
        LocalDateTime createdAt
) {
}
