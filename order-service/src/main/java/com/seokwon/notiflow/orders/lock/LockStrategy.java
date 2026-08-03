package com.seokwon.notiflow.orders.lock;

/**
 * 재고 차감 시 동시성 제어 전략
 * @description 같은 상품(SKU)에 여러 요청이 몰릴 때 재고를 어떻게 순차화할지 선택하는 옵션.
 * 셋 다 "최종적으로 재고가 정확히 줄어드는가"는 같지만, 락을 어디서(DB vs 애플리케이션 레이어) 잡는지가 다르다.
 */
public enum LockStrategy {
    /** 락 없음 - 조회 후 차감을 그냥 순서대로 실행. 동시 요청 시 lost-update(오버셀)가 재현되는 비교군 */
    NONE,
    /** DB 비관적 락(SELECT ... FOR UPDATE) - 같은 행에 대한 다른 트랜잭션의 조회 자체를 커밋될 때까지 블로킹 */
    PESSIMISTIC,
    /** Redisson 분산락 - DB가 아니라 Redis에 "이 상품(detailId)은 지금 누가 처리 중"이라는 락을 걸어 직렬화.
     * DB 락과 결과는 동일하지만, 여러 서비스 인스턴스/여러 리소스에 걸친 락이 필요한 실무 상황을 재현. */
    DISTRIBUTED
}
