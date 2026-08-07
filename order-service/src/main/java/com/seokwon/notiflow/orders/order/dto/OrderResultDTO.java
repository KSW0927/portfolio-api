package com.seokwon.notiflow.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import com.seokwon.notiflow.orders.order.OrderStatus;

@Getter
@AllArgsConstructor
public class OrderResultDTO {
    private Long orderId;
    private Long detailId;
    private String model;
    private String storage;
    private String color;
    private Long buyerUserNo;
    private boolean success;
    private OrderStatus status;

    /**
     * 서버가 이 요청을 처리하는 데 걸린 시간(ms) - 락 획득 대기시간까지 포함.
     * 프론트가 예전엔 요청~응답 왕복시간(performance.now())을 응답시간으로 썼는데,
     * 그건 락 전략과 무관한 네트워크 지연까지 섞여서 락 전략별 비교 지표로는 부정확했다.
     * OrderController가 (DISTRIBUTED의 Redis 락 대기 포함) 실제 처리 구간을 재서 여기 채워준다.
     * 생성 시점엔 알 수 없어 별도 @Setter로 컨트롤러에서 채움.
     */
    @Setter
    private Long processingMs;
}
