package com.seokwon.notiflow.order.event;

import java.time.LocalDateTime;
import java.util.List;

import com.seokwon.notiflow.order.entity.LockStrategy;

/**
 * 배치 종료 후 재고 정합성 체크 결과 이벤트
 * @description OrderPlacedEvent(건별 주문)와 달리 배치 전체를 요약한 이벤트.
 * lostUnits > 0이면 NONE(락 미적용) 상태에서 lost-update(오버셀)가 실제로 발생했다는 뜻.
 */
public record StockIntegrityEvent(
        Integer expectedTotal,
        Integer actualTotal,
        Integer lostUnits,
        LockStrategy lockStrategy,
        List<OversoldProduct> oversoldProducts,
        LocalDateTime occurredAt
) {
}
