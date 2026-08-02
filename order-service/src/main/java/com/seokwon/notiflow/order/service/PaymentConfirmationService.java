package com.seokwon.notiflow.order.service;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.seokwon.notiflow.order.entity.OrderEntity;
import com.seokwon.notiflow.order.entity.PaymentStatus;
import com.seokwon.notiflow.order.event.PaymentConfirmedEvent;
import com.seokwon.notiflow.order.repository.OrderRepository;

/**
 * 결제 확정 처리 전용 서비스
 * @description OrderService.placeOrder가 예약해둔 타이머(TaskScheduler)가 지연시간 경과 후 호출한다.
 * OrderService 안에 같이 두지 않고 별도 빈으로 분리한 이유는, 스케줄된 람다가 "같은 빈 안의 다른
 * @Transactional 메서드"를 호출하는 self-invocation 패턴이 되면 프록시가 가로채지 못해 트랜잭션이
 * 아예 안 걸리기 때문(OrderService 내부에서 한 번 겪었던 문제와 동일) - 별도 빈으로 두면 항상
 * 프록시를 거쳐 호출되므로 이 문제가 생기지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfirmationService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void confirmPayment(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        // 이미 처리(결제완료/취소)됐거나, 재고 초기화로 삭제된 건은 건너뜀
        if (order == null || order.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setPaymentConfirmedAt(now);

        eventPublisher.publishEvent(new PaymentConfirmedEvent(
                order.getOrderId(),
                order.getBuyer().getUserNo(),
                order.getProductDetail().getDetailId(),
                order.getProductDetail().getProduct().getModel(),
                order.getProductDetail().getStorage(),
                order.getProductDetail().getColor(),
                now
        ));
    }
}
