package com.seokwon.notiflow.notify.event;

import java.time.LocalDateTime;

/**
 * DB 저장이 끝난 알림을 실시간으로 내보내기 위한 이벤트
 * OrderPlacedEvent(주문 도메인 원본)와 달리, 이미 사람이 읽을 문구로 가공되고
 * notifyId가 채번된 "표시용" 데이터. realtime-gateway-service가 이 토픽만 구독하면
 * 주문 도메인을 몰라도 그대로 화면에 뿌릴 수 있음.
 * 필드명은 realtime-gateway-service의 자체 사본 및 프론트 notificationStore가 그대로 참조하는
 * wire contract라, 이 클래스를 바꿀 때는 반드시 그 두 곳도 같이 맞춰야 함.
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
