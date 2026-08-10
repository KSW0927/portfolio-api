package com.seokwon.notiflow.orders.order;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.seokwon.notiflow.orders.customer.CustomerEntity;
import com.seokwon.notiflow.orders.event.PaymentConfirmedEvent;
import com.seokwon.notiflow.orders.product.ProductDetailEntity;
import com.seokwon.notiflow.orders.product.ProductEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * PaymentConfirmationService 단위 테스트
 * OrderRepository/이벤트 발행자를 mock 처리해서 스케줄러 없이도
 * "결제대기(PENDING) 건만 확정 처리한다"는 핵심 가드 조건을 검증한다.
 * 특히 오버셀 사후 취소로 이미 CANCELLED된 건을, 뒤늦게 도착한 결제확정 타이머가
 * 다시 COMPLETED로 되돌리지 않는지가 이 서비스에서 가장 중요하게 지켜져야 하는 불변조건이다
 * (OrderService.cancelOversoldOrders가 PENDING/COMPLETED를 가리지 않고 CANCELLED로 덮어쓰는 이유이기도 함).
 */
@ExtendWith(MockitoExtension.class)
class PaymentConfirmationServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentConfirmationService paymentConfirmationService;

    private CustomerEntity buyer;
    private ProductDetailEntity detail;

    @BeforeEach
    void setUp() {
        buyer = CustomerEntity.builder().id(1L).userNo(100L).username("테스트구매자").build();
        ProductEntity product = ProductEntity.builder().productId(1L).model("Galaxy Z Flip8").build();
        detail = ProductDetailEntity.builder().detailId(10L).product(product).storage("256GB").color("Black").stock(4).build();
    }

    @Test
    void PENDING_상태의_주문이면_결제완료로_확정하고_PaymentConfirmedEvent를_발행한다() {
        OrderEntity order = OrderEntity.builder()
                .orderId(101L).buyer(buyer).productDetail(detail)
                .status(OrderStatus.SUCCESS).paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();
        given(orderRepository.findById(101L)).willReturn(Optional.of(order));

        paymentConfirmationService.confirmPayment(101L);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(order.getPaymentConfirmedAt()).isNotNull();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(PaymentConfirmedEvent.class);

        PaymentConfirmedEvent event = (PaymentConfirmedEvent) captor.getValue();
        assertThat(event.orderId()).isEqualTo(101L);
        assertThat(event.buyerUserNo()).isEqualTo(buyer.getUserNo());
        assertThat(event.detailId()).isEqualTo(detail.getDetailId());
        assertThat(event.model()).isEqualTo("Galaxy Z Flip8");
        assertThat(event.storage()).isEqualTo("256GB");
        assertThat(event.color()).isEqualTo("Black");
    }

    @Test
    void 주문을_찾지_못하면_아무것도_하지_않는다() {
        given(orderRepository.findById(999L)).willReturn(Optional.empty());

        paymentConfirmationService.confirmPayment(999L);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void 이미_결제완료된_주문이면_다시_처리하지_않는다() {
        LocalDateTime confirmedAt = LocalDateTime.now().minusSeconds(10);
        OrderEntity order = OrderEntity.builder()
                .orderId(101L).buyer(buyer).productDetail(detail)
                .status(OrderStatus.SUCCESS).paymentStatus(PaymentStatus.COMPLETED)
                .paymentConfirmedAt(confirmedAt)
                .createdAt(LocalDateTime.now()).build();
        given(orderRepository.findById(101L)).willReturn(Optional.of(order));

        paymentConfirmationService.confirmPayment(101L);

        // 기존 확정 시각이 덮어써지지 않아야 한다
        assertThat(order.getPaymentConfirmedAt()).isEqualTo(confirmedAt);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void 오버셀로_취소된_주문이면_뒤늦게_도착한_타이머가_결제완료로_되돌리지_않는다() {
        OrderEntity order = OrderEntity.builder()
                .orderId(101L).buyer(buyer).productDetail(detail)
                .status(OrderStatus.CANCELLED).paymentStatus(PaymentStatus.CANCELLED)
                .createdAt(LocalDateTime.now()).build();
        given(orderRepository.findById(101L)).willReturn(Optional.of(order));

        paymentConfirmationService.confirmPayment(101L);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order.getPaymentConfirmedAt()).isNull();
        verifyNoInteractions(eventPublisher);
    }
}
