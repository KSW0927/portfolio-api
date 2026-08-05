package com.seokwon.notiflow.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket 설정
 * @description 프론트는 SockJS로 /ws에 접속해서 "/topic/notifications"를 구독한다.
 * 프론트에서 서버로 보낼 메시지는 아직 없어서(수신 전용) setApplicationDestinationPrefixes는 생략.
 * 허용 origin은 common 모듈 CORS 설정(CommonSecurityConfig)과 동일한 cors.allowed-origin 프로퍼티를 씀.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigin)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }
}
