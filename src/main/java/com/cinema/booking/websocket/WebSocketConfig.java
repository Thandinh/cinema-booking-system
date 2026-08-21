package com.cinema.booking.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình WebSocket dùng giao thức STOMP (Simple Text Oriented Messaging Protocol).
 *
 * Cách frontend kết nối:
 *   const client = new Client({ brokerURL: 'ws://localhost:8080/ws' });
 *   client.subscribe('/topic/seatmap/{showtimeId}', callback);
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> allowedOrigins;

    public WebSocketConfig(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Server sẽ push message đến các topic bắt đầu bằng /topic
        registry.enableSimpleBroker("/topic");

        // Prefix cho các message từ client gửi lên server (nếu cần 2 chiều)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint 1: SockJS — fallback cho môi trường corporate proxy, firewall chặn WS thuần
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new))
                .withSockJS();

        // Endpoint 2: Native WebSocket — cho modern browser, mobile app, không cần SockJS
        // Frontend kết nối: new Client({ brokerURL: 'ws://host/ws-native' })
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }
}
