package com.seokwon.notiflow.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 제품 상세(실제 판매 단위, SKU)
 * @description 모델(ProductEntity) + 용량 + 색상 조합별로 재고를 따로 가지고 있어서,
 * 동시 주문 테스트 시 조합마다 경합/품절 상황이 자연스럽게 분산됨.
 * Pessimistic Lock 대상은 이 엔티티(재고를 실제로 들고 있는 단위)임.
 */
@Entity
@Table(name = "product_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private String storage;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private Integer stock;
}
