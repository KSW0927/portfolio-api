package com.seokwon.notiflow.orders.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 오버셀이 발생한 상품 1건에 대한 프론트 보고 - 얼마나(lostUnits) 오버셀됐는지까지 담아서,
 * 백엔드가 상품별로 정확히 몇 건씩 취소해야 하는지 알 수 있게 한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OversoldProductDTO {

    @NotNull(message = "상품 ID를 입력해주세요")
    private Long detailId;

    @NotNull(message = "오버셀 수량을 입력해주세요")
    private Integer lostUnits;
}
