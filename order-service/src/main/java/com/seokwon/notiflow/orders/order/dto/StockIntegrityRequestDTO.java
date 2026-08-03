package com.seokwon.notiflow.orders.order.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.seokwon.notiflow.orders.lock.LockStrategy;

/**
 * 배치(시뮬레이션 1회 실행) 종료 후 프론트가 계산한 재고 정합성 결과 보고
 * @description 배치 시작 시점 재고 총합, 성공 건수 등은 프론트(orderSimulationStore)가
 * 이미 들고 있고, 배치 종료 후 실제 DB 재고를 다시 조회해 비교까지 끝낸 상태로 이 값을 보낸다.
 * order-service는 이걸 그대로 믿고 Kafka 이벤트로 발행만 함(별도 DB 저장 없음) -
 * "배치"라는 개념 자체를 백엔드가 아직 모르기 때문에(주문 API는 건별 stateless 호출) 재계산이 불가능함.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockIntegrityRequestDTO {

    @NotNull(message = "예상 재고를 입력해주세요")
    private Integer expectedTotal;

    @NotNull(message = "실제 재고를 입력해주세요")
    private Integer actualTotal;

    @NotNull(message = "유실 수량을 입력해주세요")
    private Integer lostUnits;

    @NotNull(message = "락 전략을 입력해주세요")
    private LockStrategy lockStrategy;

    /**
     * 오버셀(예상보다 재고가 덜 줄어든) 것으로 감지된 상품별 수량
     * @description 알림 상세 노출뿐 아니라, 상품별로 정확히 몇 건씩 사후 취소해야 하는지의 기준값으로도 쓰인다.
     */
    private List<OversoldProductDTO> oversoldProducts;
}
