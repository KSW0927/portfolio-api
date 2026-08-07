package com.seokwon.notiflow.orders.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.seokwon.notiflow.common.exception.NotFoundException;
import com.seokwon.notiflow.orders.customer.CustomerEntity;
import com.seokwon.notiflow.orders.customer.CustomerRepository;
import com.seokwon.notiflow.orders.event.OrderPlacedEvent;
import com.seokwon.notiflow.orders.event.PaymentScheduleRequest;
import com.seokwon.notiflow.orders.event.StockIntegrityEvent;
import com.seokwon.notiflow.orders.lock.LockStrategy;
import com.seokwon.notiflow.orders.order.dto.OrderResultDTO;
import com.seokwon.notiflow.orders.order.dto.OversoldProductDTO;
import com.seokwon.notiflow.orders.order.dto.StockIntegrityRequestDTO;
import com.seokwon.notiflow.orders.product.ProductDetailEntity;
import com.seokwon.notiflow.orders.product.ProductDetailRepository;
import com.seokwon.notiflow.orders.product.ProductEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * OrderService 단위 테스트
 * Repository/이벤트 발행자를 전부 mock 처리해서 DB·Kafka 없이도
 * 락 전략별 분기, 재고 차감/오버셀 사후 취소 같은 순수 비즈니스 로직만 빠르게 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductDetailRepository productDetailRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private CustomerEntity buyer;
    private ProductEntity product;
    private ProductDetailEntity detail;

    @BeforeEach
    void setUp() {
        buyer = CustomerEntity.builder().id(1L).userNo(100L).username("테스트구매자").build();
        product = ProductEntity.builder().productId(1L).model("Galaxy Z Flip8").build();
        detail = ProductDetailEntity.builder().detailId(10L).product(product).storage("256GB").color("Black").stock(5).build();
    }

    // ------------------------------------------------------------
    // placeOrder - 락 전략별 조회 방식 분기
    // ------------------------------------------------------------

    @Test
    void placeOrder_pessimistic는_findByIdForUpdate로_조회한다() {
        given(customerRepository.findByUserNo(100L)).willReturn(Optional.of(buyer));
        given(productDetailRepository.findByIdForUpdate(10L)).willReturn(Optional.of(detail));

        orderService.placeOrder(10L, 100L, LockStrategy.PESSIMISTIC);

        verify(productDetailRepository).findByIdForUpdate(10L);
        verify(productDetailRepository, never()).findById(any());
    }

    @Test
    void placeOrder_none은_락_없이_findById로_조회한다() {
        given(customerRepository.findByUserNo(100L)).willReturn(Optional.of(buyer));
        given(productDetailRepository.findById(10L)).willReturn(Optional.of(detail));

        orderService.placeOrder(10L, 100L, LockStrategy.NONE);

        verify(productDetailRepository).findById(10L);
        verify(productDetailRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void placeOrder_distributed도_none과_동일하게_findById로_조회한다() {
        // DISTRIBUTED는 OrderController가 Redisson 분산락으로 이미 감싸서 호출하므로
        // OrderService 안에서는 NONE과 같은 경로(락 없는 조회)를 타야 한다.
        given(customerRepository.findByUserNo(100L)).willReturn(Optional.of(buyer));
        given(productDetailRepository.findById(10L)).willReturn(Optional.of(detail));

        orderService.placeOrder(10L, 100L, LockStrategy.DISTRIBUTED);

        verify(productDetailRepository).findById(10L);
        verify(productDetailRepository, never()).findByIdForUpdate(any());
    }

    // ------------------------------------------------------------
    // placeOrder - 재고 차감 성공/품절
    // ------------------------------------------------------------

    @Test
    void placeOrder_재고가_있으면_1개_차감하고_결제대기로_저장하며_이벤트_2건을_발행한다() {
        given(customerRepository.findByUserNo(100L)).willReturn(Optional.of(buyer));
        given(productDetailRepository.findById(10L)).willReturn(Optional.of(detail));

        OrderResultDTO result = orderService.placeOrder(10L, 100L, LockStrategy.NONE);

        assertThat(detail.getStock()).isEqualTo(4);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SUCCESS);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        OrderEntity saved = orderCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.SUCCESS);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getPaymentDueAt()).isNotNull();

        // 판매 건은 결과 통보용 OrderPlacedEvent + 결제 확정 타이머 예약용 PaymentScheduleRequest, 총 2건 발행
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<Object> events = eventCaptor.getAllValues();
        assertThat(events.get(0)).isInstanceOf(OrderPlacedEvent.class);
        assertThat(events.get(1)).isInstanceOf(PaymentScheduleRequest.class);
    }

    @Test
    void placeOrder_재고가_없으면_품절_처리하고_결제_관련_이벤트는_발행하지_않는다() {
        detail.setStock(0);
        given(customerRepository.findByUserNo(100L)).willReturn(Optional.of(buyer));
        given(productDetailRepository.findById(10L)).willReturn(Optional.of(detail));

        OrderResultDTO result = orderService.placeOrder(10L, 100L, LockStrategy.NONE);

        assertThat(detail.getStock()).isZero();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.OUT_OF_STOCK);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getPaymentStatus()).isNull();

        // 품절 건은 OrderPlacedEvent 1건만 발행되고, PaymentScheduleRequest는 발행되지 않는다
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(OrderPlacedEvent.class);
    }

    // ------------------------------------------------------------
    // placeOrder - 예외
    // ------------------------------------------------------------

    @Test
    void placeOrder_구매자를_찾지_못하면_NotFoundException을_던진다() {
        given(customerRepository.findByUserNo(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(10L, 999L, LockStrategy.NONE))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(productDetailRepository, orderRepository, eventPublisher);
    }

    @Test
    void placeOrder_상품을_찾지_못하면_NotFoundException을_던진다() {
        given(customerRepository.findByUserNo(100L)).willReturn(Optional.of(buyer));
        given(productDetailRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(10L, 100L, LockStrategy.NONE))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(orderRepository, eventPublisher);
    }

    // ------------------------------------------------------------
    // reportBatchResult / 오버셀 사후 취소
    // ------------------------------------------------------------

    @Test
    void reportBatchResult_오버셀_건수만큼_최근_성공_주문을_취소하고_재고를_복구한다() {
        ProductDetailEntity oversoldDetail = ProductDetailEntity.builder()
                .detailId(20L).product(product).storage("512GB").color("White").stock(3).build();

        OrderEntity order1 = OrderEntity.builder()
                .orderId(101L).buyer(buyer).productDetail(oversoldDetail)
                .status(OrderStatus.SUCCESS).paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();
        // 이미 결제완료(COMPLETED)였던 건도 오버셀 취소 대상이면 무조건 CANCELLED로 덮어써야
        // 뒤늦게 도착한 결제확정 타이머가 되돌리는 걸 막을 수 있다.
        OrderEntity order2 = OrderEntity.builder()
                .orderId(102L).buyer(buyer).productDetail(oversoldDetail)
                .status(OrderStatus.SUCCESS).paymentStatus(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now()).build();

        given(productDetailRepository.findByIdForUpdate(20L)).willReturn(Optional.of(oversoldDetail));
        given(orderRepository.findRecentSuccessOrders(eq(20L), any())).willReturn(List.of(order1, order2));

        StockIntegrityRequestDTO dto = new StockIntegrityRequestDTO(
                10, 8, 2, LockStrategy.NONE, List.of(new OversoldProductDTO(20L, 2))
        );

        orderService.reportBatchResult(dto);

        assertThat(order1.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order1.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order2.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order2.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(oversoldDetail.getStock()).isEqualTo(5);

        // StockIntegrityEvent 1건 + 취소된 주문마다 OrderPlacedEvent(CANCELLED) 2건 = 총 3건
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(3)).publishEvent(eventCaptor.capture());
        List<Object> events = eventCaptor.getAllValues();
        assertThat(events.get(0)).isInstanceOf(StockIntegrityEvent.class);
        assertThat(events.subList(1, 3)).allSatisfy(event -> {
            assertThat(event).isInstanceOf(OrderPlacedEvent.class);
            assertThat(((OrderPlacedEvent) event).status()).isEqualTo(OrderStatus.CANCELLED);
        });
    }

    @Test
    void reportBatchResult_오버셀_수량이_0이면_취소_대상을_조회하지_않는다() {
        StockIntegrityRequestDTO dto = new StockIntegrityRequestDTO(
                10, 10, 0, LockStrategy.PESSIMISTIC, List.of(new OversoldProductDTO(20L, 0))
        );

        orderService.reportBatchResult(dto);

        verify(orderRepository, never()).findRecentSuccessOrders(any(), any());
        verify(eventPublisher, times(1)).publishEvent(any(StockIntegrityEvent.class));
    }

    @Test
    void reportBatchResult_취소_대상_주문을_찾지_못해도_예외없이_종료된다() {
        given(orderRepository.findRecentSuccessOrders(eq(20L), any())).willReturn(List.of());

        StockIntegrityRequestDTO dto = new StockIntegrityRequestDTO(
                10, 9, 1, LockStrategy.NONE, List.of(new OversoldProductDTO(20L, 1))
        );

        assertThatCode(() -> orderService.reportBatchResult(dto)).doesNotThrowAnyException();

        verify(productDetailRepository, never()).findByIdForUpdate(any());
        verify(eventPublisher, times(1)).publishEvent(any(StockIntegrityEvent.class));
    }
}
