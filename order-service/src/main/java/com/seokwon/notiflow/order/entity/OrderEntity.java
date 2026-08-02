package com.seokwon.notiflow.order.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 주문 처리 이력
 * @description 재고 차감 성공/품절 여부와 관계없이 시도된 주문을 전부 기록(부하테스트 결과 집계용).
 * 구매자(buyer)는 실제 로그인 계정이 아니라 시뮬레이션용 테스트 구매자 풀(CustomerEntity)을 참조함.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private CustomerEntity buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_id", nullable = false)
    private ProductDetailEntity productDetail;

    // columnDefinition을 직접 지정해서 Hibernate 6가 enum 값 기준으로 CHECK 제약조건을
    // 자동 생성하지 않게 함 - ddl-auto=update는 기존 제약조건을 갱신 안 해주기 때문에,
    // 나중에 enum에 값을 추가할 때마다 DB 제약조건 때문에 막히는 걸 방지(payment_status에서 겪은 문제).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 결제 진행 상태 - 판매(재고 차감 성공) 건에만 값이 채워짐(품절 건은 null)
     * @description nullable로 둬서, 기존에 이미 쌓여있는 행에 컬럼을 추가할 때
     * ddl-auto=update가 NOT NULL 제약 때문에 실패하는 문제를 피함(이전에 buyer_user_no/priority
     * 컬럼 추가 때 겪었던 것과 동일한 함정).
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private PaymentStatus paymentStatus;

    /** 결제가 확정되어야 할 예정 시각 - 구매자별 랜덤 지연(300ms~8s)을 주문 시점에 미리 계산해서 저장 */
    private LocalDateTime paymentDueAt;

    /** 실제로 결제가 확정 처리된 시각 - 스케줄러가 paymentDueAt이 지난 건을 확인해 채움 */
    private LocalDateTime paymentConfirmedAt;
}
