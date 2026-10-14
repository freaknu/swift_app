package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Verifies a native WebSocket subscriber against chat-service's authoritative
 * participant check. The WebSocket service does not duplicate chat-session
 * persistence or identity resolution locally.
 */
@Slf4j
@Component
public class ChatSessionAccessClient {
    private final RestClient client;

    public ChatSessionAccessClient(
            @Value("${services.chat-url:http://localhost:9004}") String chatUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(3000);
        client = RestClient.builder()
                .baseUrl(chatUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean belongsTo(Long authUserId, Long sessionId) {
        if (authUserId == null || authUserId <= 0 || sessionId == null || sessionId <= 0) {
            return false;
        }
        try {
            client.get()
                    .uri("/api/v1/chat/sessions/{sessionId}/access", sessionId)
                    .header("x-user-id", authUserId.toString())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.debug("Chat session {} is not available to auth user {}", sessionId, authUserId);
            return false;
        }
    }
}
