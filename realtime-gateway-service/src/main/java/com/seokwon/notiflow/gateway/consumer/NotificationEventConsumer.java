package com.seokwon.notiflow.gateway.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.seokwon.notiflow.common.kafka.KafkaTopics;
import com.seokwon.notiflow.gateway.event.NotificationPublishedEvent;

/**
 * notification-events 토픽을 구독해서 받은 즉시 STOMP로 브로드캐스트
 * @description 별도의 저장/가공 없이 그대로 전달만 함 - 가공은 이미 notify-service가 끝냈음.
 * 프론트는 "/topic/notifications"를 구독하고 있으면 이 메시지를 실시간으로 받는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private static final String DESTINATION = "/topic/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS)
    public void onNotificationPublished(NotificationPublishedEvent event) {
        messagingTemplate.convertAndSend(DESTINATION, event);
        log.debug("알림 브로드캐스트: notificationId={}", event.notificationId());
    }
}
