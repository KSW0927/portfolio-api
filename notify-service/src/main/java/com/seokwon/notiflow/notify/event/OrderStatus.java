package com.seokwon.notiflow.notify.event;

/**
 * 주문 처리 결과 상태
 * @description order-service의 OrderStatus와 값이 동일해야 함(계약).
 * 서비스 간 결합을 피하기 위해 클래스를 공유하지 않고 notify-service가 자체적으로 보유.
 */
public enum OrderStatus {
    SUCCESS,        // 정상 처리(재고 차감 성공)
    OUT_OF_STOCK,   // 품절(재고 부족으로 실패)
    CANCELLED       // 오버셀 사후 취소
}
