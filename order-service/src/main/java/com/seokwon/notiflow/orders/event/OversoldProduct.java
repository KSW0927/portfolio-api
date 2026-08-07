package com.seokwon.notiflow.orders.event;

/**
 * 오버셀이 발생한 상품 1건 - 얼마나(lostUnits) 오버셀됐는지까지 같이 담는다.
 * 취소 실행 시 "이 상품에서 최근 성공 주문 lostUnits건을 취소한다"의 기준값으로 쓰임.
 */
public record OversoldProduct(
        Long detailId,
        Integer lostUnits
) {
}
