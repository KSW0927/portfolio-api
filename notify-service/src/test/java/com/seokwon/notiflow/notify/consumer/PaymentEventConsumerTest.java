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
import com.seokwon.notiflow.notify.event.PaymentConfirmedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * PaymentEventConsumer 단위 테스트
 * payment-events 토픽 메시지(PaymentConfirmedEvent)를 일반(비고정) 알림으로
 * 변환·저장하고, notification-events 토픽으로 재발행하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private NotifyRepository notifyRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Test
    void 결제확정_이벤트는_결제_카테고리의_일반_알림으로_저장되고_실시간_발행된다() {
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(CompletableFuture.completedFuture(null));

        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                1L, 100L, 10L, "Galaxy Z Flip8", "256GB", "Black", LocalDateTime.now()
        );

        paymentEventConsumer.onPaymentConfirmed(event);

        ArgumentCaptor<NotifyEntity> entityCaptor = ArgumentCaptor.forClass(NotifyEntity.class);
        verify(notifyRepository).save(entityCaptor.capture());
        NotifyEntity saved = entityCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(1L);
        assertThat(saved.getBuyerUserNo()).isEqualTo(100L);
        // 결제완료는 품절/오버셀과 달리 최상단 고정 노출 대상이 아니다
        assertThat(saved.isPriority()).isFalse();
        assertThat(saved.getCategory()).isEqualTo("결제");
        assertThat(saved.getMessage()).contains("결제가 확정되었습니다");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.NOTIFICATION_EVENTS), anyString(), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isInstanceOf(NotifyPublishedEvent.class);
        assertThat(((NotifyPublishedEvent) payloadCaptor.getValue()).priority()).isFalse();
    }
}
