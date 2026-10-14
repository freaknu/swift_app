package com.swiftapp_backend.websocketservice.realtime.infrastructure.realtime.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RedisSubscriberChatTest {

    @Test
    void routesChatMessagesToSessionDestination() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber subscriber = new RedisSubscriber(messagingTemplate, new ObjectMapper());
        Map<String, Object> message = Map.of("messageId", "msg-1", "sessionId", 101L);

        subscriber.receiveMessage(message);

        verify(messagingTemplate).convertAndSend("/topic/chat/session/101", message);
        verify(messagingTemplate, never()).convertAndSend("/topic/rides", message);
    }
}
