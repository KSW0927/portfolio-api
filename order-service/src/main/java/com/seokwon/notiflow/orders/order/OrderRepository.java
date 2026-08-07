package com.seokwon.notiflow.orders.order;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.seokwon.notiflow.orders.order.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * 오버셀 사후 취소 대상 조회
     * 특정 상품에서 재고 차감에 성공(SUCCESS)한 주문 중 가장 최근 것부터 골라온다.
     * "누가 진짜 오버셀의 원인인지"는 알 수 없으므로, 최근 주문부터 취소하는 FIFO(선주문 우선 보장) 방식으로
     * 정한다 - 실무에서 오버셀 발생 시 뒤늦게 확정된 주문을 취소하는 것과 같은 원리.
     */
    @Query("select o from OrderEntity o where o.productDetail.detailId = :detailId and o.status = com.seokwon.notiflow.orders.order.OrderStatus.SUCCESS order by o.createdAt desc")
    List<OrderEntity> findRecentSuccessOrders(@Param("detailId") Long detailId, Pageable pageable);
}
