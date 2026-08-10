package com.seokwon.notiflow.notify.consumer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.seokwon.notiflow.notify.NotifyEntity;
import com.seokwon.notiflow.notify.NotifyRepository;
import com.seokwon.notiflow.notify.event.LockStrategy;
import com.seokwon.notiflow.notify.event.OversoldProduct;
import com.seokwon.notiflow.notify.event.StockIntegrityEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * StockIntegrityEventConsumer 단위 테스트
 * 배치 재고 정합성 결과(StockIntegrityEvent)를 오버셀 여부에 따라
 * 어떤 문구/우선순위로 저장하는지, 특정 주문/구매자에 속하지 않는 배치 알림 특성(orderId/buyerUserNo=0)이 지켜지는지를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class StockIntegrityEventConsumerTest {

    @Mock
    private NotifyRepository notifyRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private StockIntegrityEventConsumer stockIntegrityEventConsumer;

    @Test
    void 오버셀이_없으면_일반_우선순위로_정합성_일치_문구를_저장한다() {
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(CompletableFuture.completedFuture(null));

        StockIntegrityEvent event = new StockIntegrityEvent(
                50, 50, 0, LockStrategy.PESSIMISTIC, List.of(), LocalDateTime.now()
        );

        stockIntegrityEventConsumer.onStockIntegrityEvent(event);

        ArgumentCaptor<NotifyEntity> captor = ArgumentCaptor.forClass(NotifyEntity.class);
        verify(notifyRepository).save(captor.capture());
        NotifyEntity saved = captor.getValue();
        // 배치 요약 알림은 특정 주문/구매자에 속하지 않으므로 NOT NULL 컬럼에 sentinel 0을 채운다
        assertThat(saved.getOrderId()).isZero();
        assertThat(saved.getBuyerUserNo()).isZero();
        assertThat(saved.isPriority()).isFalse();
        assertThat(saved.getCategory()).isEqualTo("재고");
        assertThat(saved.getMessage())
                .contains("재고 정합성 일치")
                .contains("DB 락")
                .contains("예상 50개")
                .contains("실제 50개");
    }

    @Test
    void 오버셀이_있으면_최상단_고정_노출과_함께_상품별_취소_내역까지_문구에_포함한다() {
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(CompletableFuture.completedFuture(null));

        StockIntegrityEvent event = new StockIntegrityEvent(
                50, 47, 3, LockStrategy.NONE,
                List.of(new OversoldProduct(20L, 3)),
                LocalDateTime.now()
        );

        stockIntegrityEventConsumer.onStockIntegrityEvent(event);

        ArgumentCaptor<NotifyEntity> captor = ArgumentCaptor.forClass(NotifyEntity.class);
        verify(notifyRepository).save(captor.capture());
        NotifyEntity saved = captor.getValue();
        assertThat(saved.isPriority()).isTrue();
        assertThat(saved.getMessage())
                .contains("오버셀 3개 발생")
                .contains("락 없음")
                .contains("제품ID: 20(3개)")
                .contains("자동 취소 처리");
    }

    @Test
    void lockStrategy가_null이면_락_없음으로_취급한다() {
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(CompletableFuture.completedFuture(null));

        StockIntegrityEvent event = new StockIntegrityEvent(
                10, 10, 0, null, List.of(), LocalDateTime.now()
        );

        stockIntegrityEventConsumer.onStockIntegrityEvent(event);

        ArgumentCaptor<NotifyEntity> captor = ArgumentCaptor.forClass(NotifyEntity.class);
        verify(notifyRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).contains("락 없음");
    }
}
