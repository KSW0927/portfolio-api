package com.seokwon.notiflow.notify.event;

/**
 * order-service가 발행하는 오버셀 상품 정보의 자체 사본
 * @description order-service/event/OversoldProduct와 필드가 동일해야 하는 계약.
 */
public record OversoldProduct(
        Long detailId,
        Integer lostUnits
) {
}
