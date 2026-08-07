package com.seokwon.notiflow.common.kafka;

/**
 * Kafka 토픽 이름 상수
 * Producer(order-service)와 Consumer(notify-service)가
 * 같은 문자열을 오타 없이 공유하도록 common 모듈에 모아둠.
 */
public final class KafkaTopics {

    /** 주문 처리 결과 이벤트 (order-service → notify-service) */
    public static final String ORDER_EVENTS = "order-events";

    /** 저장 완료된 알림 이벤트 (notify-service → realtime-gateway-service) */
    public static final String NOTIFICATION_EVENTS = "notification-events";

    /** 배치(시뮬레이션 1회 실행) 종료 후 재고 정합성 체크 결과 (order-service → notify-service) */
    public static final String STOCK_INTEGRITY_EVENTS = "stock-integrity-events";

    /** 결제 확정 이벤트 - 판매 건이 구매자별 랜덤 지연 후 결제 확정될 때 발행 (order-service → notify-service) */
    public static final String PAYMENT_EVENTS = "payment-events";

    private KafkaTopics() {
    }
}
