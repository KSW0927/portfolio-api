package com.seokwon.notiflow.notify.event;

import java.time.LocalDateTime;

/**
 * 주문 처리 결과 이벤트 (order-service가 발행)
 * order-service의 OrderPlacedEvent와 필드가 동일해야 하는 "계약"이지만,
 * 클래스 자체는 공유하지 않고 notify-service가 자체 사본을 보유한다.
 * Kafka 메시지는 타입 헤더 없이 순수 JSON 구조로만 오므로(양쪽 application.properties의
 * spring.json 설정 참고), 필드 이름/타입만 맞으면 이 클래스로 정상 역직렬화된다.
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
