package com.seokwon.notiflow.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.seokwon.notiflow.order.entity.ProductDetailEntity;

public interface ProductDetailRepository extends JpaRepository<ProductDetailEntity, Long> {

    /**
     * 재고 차감용 비관적 락 조회
     * @description 트랜잭션 종료(commit/rollback) 전까지 같은 detailId 행에 대한 다른 트랜잭션의 조회를 블로킹함.
     * 동시에 여러 주문이 같은 SKU를 두고 경합해도 순차적으로만 재고가 줄어들도록 보장(오버셀 방지).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pd from ProductDetailEntity pd where pd.detailId = :detailId")
    Optional<ProductDetailEntity> findByIdForUpdate(@Param("detailId") Long detailId);

    /**
     * 상품 목록 조회용 - N+1 방지를 위해 product를 fetch join
     */
    @Query("select pd from ProductDetailEntity pd join fetch pd.product")
    List<ProductDetailEntity> findAllWithProduct();
}
