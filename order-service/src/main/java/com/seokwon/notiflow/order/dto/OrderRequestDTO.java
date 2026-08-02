package com.seokwon.notiflow.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.seokwon.notiflow.order.entity.LockStrategy;

/**
 * 주문 요청
 * @description Authorization 헤더의 JWT는 "이 요청을 보낸 세션이 인증됐는지"만 증명한다.
 * 실제 주문 소유자(buyerUserNo)는 시뮬레이션용 테스트 구매자 풀(2000명) 중
 * 프론트가 랜덤으로 골라서 body로 전달 - 동시에 여러 명이 주문하는 상황을 재현하기 위함.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

    @NotNull(message = "제품상세 ID를 입력해주세요")
    private Long productDetailId;

    @NotNull(message = "구매자 번호를 입력해주세요")
    private Long buyerUserNo;

    /**
     * 동시성 제어 전략
     * @description NONE/PESSIMISTIC/DISTRIBUTED 중 하나. 값이 없으면(null) 기존 기본 동작과
     * 동일하게 PESSIMISTIC으로 간주.
     */
    private LockStrategy lockStrategy;
}
