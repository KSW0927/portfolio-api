package com.seokwon.notiflow.order.event;

import java.time.LocalDateTime;

import com.seokwon.notiflow.order.entity.OrderStatus;

/**
 * 주문 처리 결과 이벤트
 * @description order-service가 주문 1건을 처리할 때마다 Kafka로 발행하는 도메인 이벤트.
 * API 응답 DTO(OrderResultDTO)와 스키마가 같아 보여도, 이벤트 계약은 API 응답과 별개로 독립적으로
 * 관리하기 위해 별도 클래스로 분리함(둘 중 하나만 바뀌어도 서로 영향 없게).
 * notify-service가 이 토픽(order-events)을 구독해서 알림으로 가공한다.
 */
public record OrderPlacedEvent(
        Long orderId,
        Long detailId,
        String model,
        String storage,
        String color,
        Long buyerUserNo,
        OrderStatus status,
        LocalDateTime occurredAt
) {
}
