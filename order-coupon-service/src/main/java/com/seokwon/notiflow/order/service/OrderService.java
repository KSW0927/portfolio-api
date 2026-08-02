package com.seokwon.notiflow.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.seokwon.notiflow.common.exception.NotFoundException;
import com.seokwon.notiflow.order.dto.OrderResultDTO;
import com.seokwon.notiflow.order.entity.CustomerEntity;
import com.seokwon.notiflow.order.entity.OrderEntity;
import com.seokwon.notiflow.order.entity.OrderStatus;
import com.seokwon.notiflow.order.entity.ProductDetailEntity;
import com.seokwon.notiflow.order.repository.CustomerRepository;
import com.seokwon.notiflow.order.repository.OrderRepository;
import com.seokwon.notiflow.order.repository.ProductDetailRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

    // ProductSeeder와 동일한 초기 재고 범위(20~60)
    private static final int MIN_STOCK = 20;
    private static final int STOCK_RANGE = 60;

    // 락 적용/미적용 동일 조건으로 테스트 하기 위한 강제 딜레이
    private static final long DEMO_DELAY_MS = 40;

    private final ProductDetailRepository productDetailRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    /**
     * 주문 처리(재고 차감)
     * useLock=true(기본)면 Pessimistic Write Lock으로 같은 SKU(product_detail)에 대한
     * 동시 요청을 순차화해서 재고 정합성을 보장한다. useLock=false면 락 없이 조회 후 차감하므로,
     * 동시 요청이 몰릴 때 lost-update(오버셀)가 재현될 수 있다 - 락의 효과를 비교 시연하기 위한 옵션.
     * 재고가 있으면 1개 차감 후 성공 처리, 없으면 품절로 기록.
     * 구매자는 요청을 보낸 세션(JWT로 인증됨)이 아니라, body로 넘어온 테스트 구매자 풀 중 하나(buyerUserNo).
     */
    @Transactional
    public OrderResultDTO placeOrder(Long detailId, Long buyerUserNo, boolean useLock) {
        CustomerEntity buyer = customerRepository.findByUserNo(buyerUserNo)
                .orElseThrow(() -> new NotFoundException("구매자를 찾을 수 없습니다. userNo=" + buyerUserNo));

        ProductDetailEntity detail = (useLock
                ? productDetailRepository.findByIdForUpdate(detailId)
                : productDetailRepository.findById(detailId))
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다. detailId=" + detailId));

        sleepForDemo();

        boolean success = detail.getStock() > 0;
        if (success) {
            detail.setStock(detail.getStock() - 1);
        }

        OrderStatus status = success ? OrderStatus.SUCCESS : OrderStatus.OUT_OF_STOCK;

        OrderEntity order = OrderEntity.builder()
                .buyer(buyer)
                .productDetail(detail)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        orderRepository.save(order);

        return new OrderResultDTO(
                order.getOrderId(),
                detail.getDetailId(),
                detail.getProduct().getModel(),
                detail.getStorage(),
                detail.getColor(),
                buyer.getUserNo(),
                success,
                status
        );
    }

    private void sleepForDemo() {
        try {
            Thread.sleep(DEMO_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 재고/주문 초기화
     * 시딩 때와 동일한 범위(20~60)로 랜덤 부여함.
     */
    @Transactional
    public void resetAll() {
        orderRepository.deleteAllInBatch();

        List<ProductDetailEntity> details = productDetailRepository.findAll();
        Random random = new Random();
        for (ProductDetailEntity detail : details) {
            detail.setStock(MIN_STOCK + random.nextInt(STOCK_RANGE));
        }
    }
}
