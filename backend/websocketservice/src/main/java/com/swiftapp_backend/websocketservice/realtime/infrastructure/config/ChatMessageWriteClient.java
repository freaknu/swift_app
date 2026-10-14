package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import com.swiftapp_backend.websocketservice.realtime.api.dto.ChatMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Legacy STOMP writes must use the same durable authorization boundary as REST. */
@Component
public class ChatMessageWriteClient {
    private final RestClient client;

    @Autowired
    public ChatMessageWriteClient(
            @Value("${services.chat-url:http://localhost:9004}") String chatUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(chatUrl).requestFactory(factory).build();
    }

    ChatMessageWriteClient(RestClient client) {
        this.client = client;
    }

    public void send(Long authUserId, ChatMessageDto dto, boolean reply) {
        if (authUserId == null || authUserId <= 0 || dto == null
                || dto.getSessionId() == null || dto.getSessionId() <= 0) {
            throw new MessagingException("Authenticated chat identity and session are required");
        }
        if (reply && (dto.getReplyOfMessageId() == null || dto.getReplyOfMessageId().isBlank())) {
            throw new MessagingException("Reply target is required");
        }
        // Never forward client-controlled sender, deletion, or read-state fields.
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("messageId", dto.getMessageId());
        request.put("messageContent", dto.getMessageContent());
        request.put("messageUrl", dto.getMessageUrl());
        request.put("contentType", dto.getContentType());
        request.put("replyOfMessageId", dto.getReplyOfMessageId());
        try {
            client.post()
                    .uri("/api/v1/chat/sessions/{sessionId}/messages", dto.getSessionId())
                    .header("x-user-id", authUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new MessagingException("Chat write was not accepted by chat-service", ex);
        }
    }
}
