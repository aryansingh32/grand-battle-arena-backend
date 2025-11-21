package com.esport.EsportTournament.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

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
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure properly in production
                .withSockJS(); // Fallback for browsers without WebSocket support

        log.info("✅ WebSocket STOMP endpoints registered at /ws");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(128 * 1024) // 128 KB
                .setSendBufferSizeLimit(512 * 1024) // 512 KB
                .setSendTimeLimit(20000); // 20 seconds
    }
}