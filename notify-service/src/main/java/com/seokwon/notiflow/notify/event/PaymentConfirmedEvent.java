package com.seokwon.notiflow.notify.event;

import java.time.LocalDateTime;

/**
 * order-service가 발행하는 결제 확정 이벤트의 자체 사본
 * @description order-service/event/PaymentConfirmedEvent와 필드가 동일해야 하는 계약.
 * 서비스 간 클래스 공유 없이 각자 사본을 두는 이유는 다른 이벤트들과 동일(느슨한 결합).
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
