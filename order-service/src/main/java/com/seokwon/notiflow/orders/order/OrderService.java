package com.seokwon.notiflow.orders.order;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.seokwon.notiflow.common.exception.NotFoundException;
import com.seokwon.notiflow.orders.order.dto.OrderResultDTO;
import com.seokwon.notiflow.orders.order.dto.OversoldProductDTO;
import com.seokwon.notiflow.orders.order.dto.StockIntegrityRequestDTO;
import com.seokwon.notiflow.orders.customer.CustomerEntity;
import com.seokwon.notiflow.orders.lock.LockStrategy;
import com.seokwon.notiflow.orders.order.OrderEntity;
import com.seokwon.notiflow.orders.order.OrderStatus;
import com.seokwon.notiflow.orders.order.PaymentStatus;
import com.seokwon.notiflow.orders.product.ProductDetailEntity;
import com.seokwon.notiflow.orders.event.OrderPlacedEvent;
import com.seokwon.notiflow.orders.event.OversoldProduct;
import com.seokwon.notiflow.orders.event.PaymentScheduleRequest;
import com.seokwon.notiflow.orders.event.StockIntegrityEvent;
import com.seokwon.notiflow.orders.customer.CustomerRepository;
import com.seokwon.notiflow.orders.order.OrderRepository;
import com.seokwon.notiflow.orders.product.ProductDetailRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    // ProductSeeder와 동일한 초기 재고 범위(20~60)
    private static final int MIN_STOCK = 20;
    private static final int STOCK_RANGE = 60;

    // 락 적용/미적용 동일 조건으로 테스트 하기 위한 강제 딜레이
    private static final long DEMO_DELAY_MS = 40;

    // 결제 확정까지 걸리는 구매자별 랜덤 지연 범위 - ms 단위부터 s 단위까지 폭넓게 재현
    private static final long MIN_PAYMENT_DELAY_MS = 300;
    private static final long MAX_PAYMENT_DELAY_MS = 8_000;

    private final ProductDetailRepository productDetailRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    // Kafka로 직접 보내지 않고 Spring 이벤트로 발행 - 트랜잭션이 커밋된 뒤에만 실제 전송되도록
    // OrderEventPublisher(@TransactionalEventListener)가 받아서 처리함(아래 클래스 설명 참고).
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 처리(재고 차감)
     * lockStrategy에 따라 동시 요청을 어떻게 순차화할지가 갈린다:
     * - PESSIMISTIC: DB Pessimistic Write Lock(SELECT ... FOR UPDATE)으로 같은 SKU(product_detail)에 대한
     *   동시 요청을 순차화.
     * - NONE: 락 없이 조회 후 차감 - 동시 요청이 몰릴 때 lost-update(오버셀)가 재현될 수 있다(비교 시연용).
     * - DISTRIBUTED: 호출부(OrderController)가 이미 Redisson 분산락으로 감싸서 호출하므로, 여기서는
     *   NONE과 동일하게 락 없이 조회한다(직렬화는 이 메서드 바깥에서 이미 끝난 상태).
     * 재고가 있으면 1개 차감 후 성공 처리, 없으면 품절로 기록.
     * 구매자는 요청을 보낸 세션(JWT로 인증됨)이 아니라, body로 넘어온 테스트 구매자 풀 중 하나(buyerUserNo).
     */
    @Transactional
    public OrderResultDTO placeOrder(Long detailId, Long buyerUserNo, LockStrategy lockStrategy) {
        CustomerEntity buyer = customerRepository.findByUserNo(buyerUserNo)
                .orElseThrow(() -> new NotFoundException("구매자를 찾을 수 없습니다. userNo=" + buyerUserNo));

        ProductDetailEntity detail = (lockStrategy == LockStrategy.PESSIMISTIC
                ? productDetailRepository.findByIdForUpdate(detailId)
                : productDetailRepository.findById(detailId))
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다. detailId=" + detailId));

        sleepForDemo();

        boolean success = detail.getStock() > 0;
        if (success) {
            detail.setStock(detail.getStock() - 1);
        }

        OrderStatus status = success ? OrderStatus.SUCCESS : OrderStatus.OUT_OF_STOCK;
        LocalDateTime now = LocalDateTime.now();

        OrderEntity.OrderEntityBuilder orderBuilder = OrderEntity.builder()
                .buyer(buyer)
                .productDetail(detail)
                .status(status)
                .createdAt(now);

        // 품절 건은 결제 대상이 아니므로 결제 관련 필드는 비워둔다(null).
        // 판매 건만 구매자별 랜덤 지연(300ms~8s) 뒤 결제가 확정되도록 예정 시각을 미리 계산해서 저장.
        long delayMs = 0;
        if (success) {
            delayMs = ThreadLocalRandom.current().nextLong(MIN_PAYMENT_DELAY_MS, MAX_PAYMENT_DELAY_MS + 1);
            orderBuilder.paymentStatus(PaymentStatus.PENDING)
                    .paymentDueAt(now.plus(delayMs, ChronoUnit.MILLIS));
        }

        OrderEntity order = orderBuilder.build();
        orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPlacedEvent(
                order.getOrderId(),
                detail.getDetailId(),
                detail.getProduct().getModel(),
                detail.getStorage(),
                detail.getColor(),
                buyer.getUserNo(),
                status,
                order.getCreatedAt()
        ));

        // DB 폴링 대신, 이 순간 정확한 지연시간을 알고 있으니 그 시간 뒤 딱 한 번만 실행될 타이머를 예약한다
        // (OrderEventPublisher.onPaymentScheduleRequested가 AFTER_COMMIT에서 실제로 타이머를 건다).
        if (success) {
            eventPublisher.publishEvent(new PaymentScheduleRequest(order.getOrderId(), delayMs));
        }

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

    /**
     * 배치(시뮬레이션 1회 실행) 종료 후 재고 정합성 결과를 처리
     * @description 프론트가 배치 시작/종료 시점 재고를 비교해 계산한 결과를 그대로 받아 알림용 이벤트를 발행하고,
     * 오버셀(lostUnits > 0)이 감지된 상품에 대해서는 실제로 사후 취소까지 수행한다.
     * "누가 오버셀의 원인인지"는 알 수 없으므로, 상품별로 가장 최근 성공 주문부터 lostUnits개를 골라
     * 취소 처리(재고 +1 복구, 주문 상태 CANCELLED, 결제 취소 알림 발행)한다 - 실무에서 오버셀 발견 시
     * 뒤늦게 확정된 주문을 취소/환불하는 것과 같은 원리(FIFO 우선순위 보장).
     */
    @Transactional
    public void reportBatchResult(StockIntegrityRequestDTO dto) {
        List<OversoldProductDTO> oversoldProducts = dto.getOversoldProducts();

        StockIntegrityEvent event = new StockIntegrityEvent(
                dto.getExpectedTotal(),
                dto.getActualTotal(),
                dto.getLostUnits(),
                dto.getLockStrategy(),
                oversoldProducts == null ? List.of() : oversoldProducts.stream()
                        .map(p -> new OversoldProduct(p.getDetailId(), p.getLostUnits()))
                        .toList(),
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);

        if (oversoldProducts != null) {
            for (OversoldProductDTO oversold : oversoldProducts) {
                cancelOversoldOrders(oversold.getDetailId(), oversold.getLostUnits());
            }
        }
    }

    private void cancelOversoldOrders(Long detailId, int cancelCount) {
        if (cancelCount <= 0) return;

        List<OrderEntity> candidates = orderRepository.findRecentSuccessOrders(detailId, PageRequest.of(0, cancelCount));
        if (candidates.isEmpty()) {
            log.warn("취소 대상 주문을 찾지 못함: detailId={}, cancelCount={}", detailId, cancelCount);
            return;
        }

        // 재고 복구는 락을 걸고 진행 - 다음 배치가 이미 시작돼서 같은 상품에 동시 접근할 가능성을 대비
        ProductDetailEntity detail = productDetailRepository.findByIdForUpdate(detailId).orElse(null);

        for (OrderEntity order : candidates) {
            order.setStatus(OrderStatus.CANCELLED);
            // PENDING이었든 COMPLETED였든 상관없이 취소로 덮어써서, 결제 확정 스케줄러가 이 건을
            // 뒤늦게 다시 집어서 '결제완료'로 되돌리는 걸 막는다.
            order.setPaymentStatus(PaymentStatus.CANCELLED);
            if (detail != null) {
                detail.setStock(detail.getStock() + 1);
            }

            eventPublisher.publishEvent(new OrderPlacedEvent(
                    order.getOrderId(),
                    order.getProductDetail().getDetailId(),
                    order.getProductDetail().getProduct().getModel(),
                    order.getProductDetail().getStorage(),
                    order.getProductDetail().getColor(),
                    order.getBuyer().getUserNo(),
                    OrderStatus.CANCELLED,
                    LocalDateTime.now()
            ));
        }

        log.info("오버셀 사후 취소 완료: detailId={}, 취소건수={}", detailId, candidates.size());
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
