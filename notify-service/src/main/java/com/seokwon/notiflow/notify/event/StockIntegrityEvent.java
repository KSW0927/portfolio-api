package com.seokwon.notiflow.notify.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * order-service가 발행하는 배치 재고 정합성 이벤트의 자체 사본
 * @description order-service/event/StockIntegrityEvent와 필드가 동일해야 하는 계약.
 * 서비스 간 클래스 공유 없이 각자 사본을 두는 이유는 다른 이벤트들과 동일(느슨한 결합).
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
