package com.seokwon.notiflow.orders.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.seokwon.notiflow.common.exception.BusinessException;
import com.seokwon.notiflow.common.response.ResponseResult;

/**
 * Redisson 분산락 실행기
 * @description 락 획득 → 주어진 작업 실행 → 락 해제를 한 곳에서 책임진다.
 * 반드시 트랜잭션(OrderService.placeOrder 등)을 시작하는 호출부보다 "바깥"에서 락을 잡고 있어야 한다.
 * 이 클래스 안에서 대상 트랜잭션 메서드를 직접 호출하지 않고 Supplier로 받아서 실행하는 이유는,
 * 호출하는 쪽(Controller 등)이 프록시를 거쳐 트랜잭션 빈을 넘겨주도록 강제하기 위함 -
 * 그래야 락 해제 시점에 이미 DB 커밋까지 끝난 상태가 보장된다(self-invocation으로 트랜잭션이
 * 걸리지 않는 문제를 이 프로젝트에서 여러 번 겪었던 것과 동일한 함정을 여기서도 피함).
 * leaseTime을 지정하지 않고 tryLock(waitTime)만 쓰는 이유는 Redisson의 watchdog이 락을 들고
 * 있는 동안 자동으로 갱신해주기 때문 - 처리 시간을 미리 못 박아두지 않아도 안전함.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private static final long WAIT_TIME_SEC = 5;

    private final RedissonClient redissonClient;

    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(WAIT_TIME_SEC, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("분산락 획득 실패(대기시간 초과): key={}", lockKey);
                throw new BusinessException(ResponseResult.ERROR_SERVER);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResponseResult.ERROR_SERVER);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
