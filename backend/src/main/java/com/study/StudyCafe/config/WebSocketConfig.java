package com.study.StudyCafe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocket 메시지 브로커 활성화 (STOMP 사용)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 주소 prefix (브로커 → 클라이언트)
        config.enableSimpleBroker("/topic");  // 예: /topic/seats/{zoneId}

        // 클라이언트가 메시지를 보낼 때 prefix (클라이언트 → 서버)
        config.setApplicationDestinationPrefixes("/app");  // 예: /app/seats/hold
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트 등록 (프론트에서 연결할 주소)
        registry.addEndpoint("/ws-seat")
                .setAllowedOriginPatterns("http://14.37.8.141:3000") // CORS 허용 (운영 시 제한 필요)
                .withSockJS(); // SockJS 폴백 지원
    }
}
