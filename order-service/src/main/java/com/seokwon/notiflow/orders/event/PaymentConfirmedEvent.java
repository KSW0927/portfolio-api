package com.seokwon.notiflow.orders.event;

import java.time.LocalDateTime;

/**
 * 결제 확정 이벤트
 * @description OrderPlacedEvent(주문 접수)와 별개로, 판매(재고 차감 성공) 건이
 * 구매자별 랜덤 지연 뒤 실제로 결제 확정 처리됐을 때 발행하는 이벤트.
 * 재고에는 영향 없는 순수 후처리 상태 전환 - notify-service가 이걸 받아
 * "결제완료" 알림으로 가공한다.
 */
public record PaymentConfirmedEvent(
        Long orderId,
        Long buyerUserNo,
        Long detailId,
        String model,
        String storage,
        String color,
        LocalDateTime confirmedAt
) {
}
