package com.seokwon.notiflow.notify.consumer;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.seokwon.notiflow.common.kafka.KafkaTopics;
import com.seokwon.notiflow.notify.NotifyEntity;
import com.seokwon.notiflow.notify.NotifyRepository;
import com.seokwon.notiflow.notify.event.NotifyPublishedEvent;
import com.seokwon.notiflow.notify.event.OrderPlacedEvent;
import com.seokwon.notiflow.notify.event.OrderStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * OrderEventConsumer 단위 테스트
 * order-events 토픽 메시지(OrderPlacedEvent)를 받아 어떤 알림 문구/우선순위/카테고리로
 * 변환·저장하는지, 그리고 notification-events 토픽으로 올바르게 재발행하는지를 검증한다.
 * Kafka/DB 없이 Repository와 KafkaTemplate을 mock 처리해서 메시지 변환 로직만 빠르게 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private NotifyRepository notifyRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    @Test
    void 판매_성공_이벤트는_일반_주문_알림으로_저장되고_실시간_발행된다() {
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(CompletableFuture.completedFuture(null));

        OrderPlacedEvent event = new OrderPlacedEvent(
                1L, 10L, "Galaxy Z Flip8", "256GB", "Black", 100L, OrderStatus.SUCCESS, LocalDateTime.now()
        );

        orderEventConsumer.onOrderEvent(event);

        ArgumentCaptor<NotifyEntity> entityCaptor = ArgumentCaptor.forClass(NotifyEntity.class);
        verify(notifyRepository).save(entityCaptor.capture());
        NotifyEntity saved = entityCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(1L);
        assertThat(saved.getBuyerUserNo()).isEqualTo(100L);
        assertThat(saved.isPriority()).isFalse();
        assertThat(saved.getCategory()).isEqualTo("주문");
        assertThat(saved.getMessage()).contains("주문이 접수되었습니다");
        assertThat(saved.isRead()).isFalse();

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.NOTIFICATION_EVENTS), anyString(), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isInstanceOf(NotifyPublishedEvent.class);
        NotifyPublishedEvent published = (NotifyPublishedEvent) payloadCaptor.getValue();
        assertThat(published.orderId()).isEqualTo(1L);
        assertThat(published.priority()).isFalse();
        assertThat(published.category()).isEqualTo("주문");
    }

    @Test
    void 품절_이벤트는_최상단_고정_노출_priority로_저장된다() {
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(CompletableFuture.completedFuture(null));

        OrderPlacedEvent event = new OrderPlacedEvent(
                2L, 10L, "Galaxy Z Flip8", "256GB", "Black", 100L, OrderStatus.OUT_OF_STOCK, LocalDateTime.now()
        );

        orderEventConsumer.onOrderEvent(event);

        ArgumentCaptor<NotifyEntity> captor = ArgumentCaptor.forClass(NotifyEntity.class);
        verify(notifyRepository).save(captor.capture());
        NotifyEntity saved = captor.getValue();
        assertThat(saved.isPriority()).isTrue();
        assertThat(saved.getCategory()).isEqualTo("주문");
        assertThat(saved.getMessage()).contains("품절로 실패했습니다");
    }

    @Test
    void 오버셀_취소_이벤트는_결제취소_카테고리로_최상단_고정_노출된다() {
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(CompletableFuture.completedFuture(null));

        OrderPlacedEvent event = new OrderPlacedEvent(
                3L, 10L, "Galaxy Z Flip8", "256GB", "Black", 100L, OrderStatus.CANCELLED, LocalDateTime.now()
        );

        orderEventConsumer.onOrderEvent(event);

        ArgumentCaptor<NotifyEntity> captor = ArgumentCaptor.forClass(NotifyEntity.class);
        verify(notifyRepository).save(captor.capture());
        NotifyEntity saved = captor.getValue();
        assertThat(saved.isPriority()).isTrue();
        assertThat(saved.getCategory()).isEqualTo("결제취소");
        assertThat(saved.getMessage()).contains("오버셀").contains("환불이 진행됩니다");
    }
}
