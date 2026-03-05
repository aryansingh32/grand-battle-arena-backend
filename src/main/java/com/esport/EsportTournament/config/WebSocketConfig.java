package com.esport.EsportTournament.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket Configuration for Real-time Updates
 * Enables live tournament updates, slot bookings, and notifications
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for broadcasting messages
        config.enableSimpleBroker(
                "/topic",    // Public broadcasts (tournament updates)
                "/queue"     // Private user messages (personal notifications)
        );

        // Set prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");

        // Set prefix for user-specific messages
        config.setUserDestinationPrefix("/user");

        log.info("✅ WebSocket message broker configured successfully");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String allowedOriginsEnv = System.getenv().getOrDefault(
                "WS_ALLOWED_ORIGIN_PATTERNS",
                "http://localhost:3000,http://localhost:5173,http://localhost:8081,http://127.0.0.1:8081,http://10.0.2.2:8081");
        List<String> allowedOrigins = Arrays.stream(allowedOriginsEnv.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toList());

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(new String[0]))
                .withSockJS(); // Fallback for browsers without WebSocket support

        log.info("✅ WebSocket STOMP endpoints registered at /ws with {} allowed origins", allowedOrigins.size());
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(128 * 1024) // 128 KB
                .setSendBufferSizeLimit(512 * 1024) // 512 KB
                .setSendTimeLimit(20000); // 20 seconds
    }
}
