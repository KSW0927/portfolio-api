package com.seokwon.notiflow.order.entity;

/**
 * 주문 처리 결과 상태
 */
public enum OrderStatus {
    SUCCESS,        // 정상 처리(재고 차감 성공)
    OUT_OF_STOCK    // 품절(재고 부족으로 실패)
}
