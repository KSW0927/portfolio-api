package com.seokwon.notiflow.orders.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import com.seokwon.notiflow.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * DistributedLockService 단위 테스트
 * RedissonClient/RLock을 mock 처리해서 실제 Redis 없이
 * 락 획득 성공/실패, action 실행 중 예외 발생, 인터럽트 상황에서도
 * "락이 반드시(그리고 정확히 필요할 때만) 해제되는지"를 검증한다.
 * 락이 안 풀리는 버그는 이후 모든 요청을 막아버리는 심각한 장애로 이어지므로
 * finally 블록의 방어 로직이 실제로 지켜지는지가 이 클래스 테스트의 핵심이다.
 */
@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    private static final String LOCK_KEY = "stock-lock:1";

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    @InjectMocks
    private DistributedLockService distributedLockService;

    @BeforeEach
    void setUp() {
        given(redissonClient.getLock(LOCK_KEY)).willReturn(lock);
    }

    @AfterEach
    void 인터럽트_테스트에서_세운_플래그가_다음_테스트로_새지_않도록_초기화한다() {
        Thread.interrupted();
    }

    @Test
    void 락_획득에_성공하면_action을_실행하고_결과를_반환한_뒤_락을_해제한다() throws InterruptedException {
        given(lock.tryLock(5L, TimeUnit.SECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);

        String result = distributedLockService.executeWithLock(LOCK_KEY, () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(lock).unlock();
    }

    @Test
    void 락_획득에_실패하면_BusinessException을_던지고_action은_실행되지_않는다() throws InterruptedException {
        given(lock.tryLock(5L, TimeUnit.SECONDS)).willReturn(false);
        Supplier<String> action = mock(Supplier.class);

        assertThatThrownBy(() -> distributedLockService.executeWithLock(LOCK_KEY, action))
                .isInstanceOf(BusinessException.class);

        verify(action, never()).get();
        verify(lock, never()).unlock();
    }

    @Test
    void action_실행_중_예외가_나도_락은_반드시_해제된다() throws InterruptedException {
        given(lock.tryLock(5L, TimeUnit.SECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);

        Supplier<String> action = () -> {
            throw new RuntimeException("boom");
        };

        assertThatThrownBy(() -> distributedLockService.executeWithLock(LOCK_KEY, action))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        verify(lock).unlock();
    }

    @Test
    void tryLock_대기중_인터럽트가_발생하면_인터럽트_상태를_보존하고_BusinessException을_던진다() throws InterruptedException {
        given(lock.tryLock(5L, TimeUnit.SECONDS)).willThrow(new InterruptedException());

        assertThatThrownBy(() -> distributedLockService.executeWithLock(LOCK_KEY, () -> "ok"))
                .isInstanceOf(BusinessException.class);

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        verify(lock, never()).unlock();
    }

    @Test
    void 락을_획득했어도_현재_스레드가_보유중이_아니면_unlock을_호출하지_않는다() throws InterruptedException {
        given(lock.tryLock(5L, TimeUnit.SECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(false);

        distributedLockService.executeWithLock(LOCK_KEY, () -> "ok");

        verify(lock, never()).unlock();
    }
}
