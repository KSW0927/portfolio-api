package com.seokwon.notiflow.notify.event;

/**
 * order-service/entity/LockStrategy의 자체 사본
 * 서비스 간 클래스 공유 없이 각자 사본을 두는 이유는 다른 이벤트/enum들과 동일(느슨한 결합).
 */
public enum LockStrategy {
    NONE,
    PESSIMISTIC,
    DISTRIBUTED
}
