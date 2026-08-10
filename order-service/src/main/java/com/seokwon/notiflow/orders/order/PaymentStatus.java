package com.seokwon.notiflow.orders.order;

/**
 * 결제 진행 상태
 * 재고 차감(성공/품절)은 주문 시점에 즉시 확정되고 바뀌지 않는다(락 경합 데모 유지).
 * 결제는 그 뒤에 붙는 후처리 단계 - 판매(재고 차감 성공) 건에 한해 구매자별 랜덤 지연 후
 * 결제가 확정되는 흐름을 재현한다. 품절 건은 결제 대상이 아니므로 이 값이 null로 남는다.
 */
public enum PaymentStatus {
    PENDING,    // 결제대기 - 재고는 이미 차감됐고, 결제 확정 대기 중
    COMPLETED,  // 결제완료 - 주문 시점에 예약된 타이머(PaymentConfirmationService)가 지연시간 경과 후 확정 처리함
    CANCELLED   // 결제취소 - 오버셀 사후 취소 대상으로 뽑힌 건. PENDING이었든 COMPLETED였든 상관없이 전환됨.
                // PaymentConfirmationService.confirmPayment가 PENDING만 대상으로 확정 처리하므로, 취소 시 반드시
                // 이 값으로 바꿔둬야 뒤늦게 도착한 타이머가 취소된 건을 "결제완료"로 되돌리는 걸 막을 수 있음.
}
