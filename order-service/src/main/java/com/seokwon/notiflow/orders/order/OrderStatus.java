package com.seokwon.notiflow.orders.order;

/**
 * 주문 처리 결과 상태
 */
public enum OrderStatus {
    SUCCESS,        // 정상 처리(재고 차감 성공)
    OUT_OF_STOCK,   // 품절(재고 부족으로 실패)
    CANCELLED       // 오버셀 사후 취소 - SUCCESS였던 건이 배치 정합성 검증에서 취소 대상으로 뽑힌 경우
}
