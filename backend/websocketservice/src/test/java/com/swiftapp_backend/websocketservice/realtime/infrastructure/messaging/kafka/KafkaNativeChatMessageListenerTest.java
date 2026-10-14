package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.websocketservice.realtime.application.port.RealtimeMessagePublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.event.MessageCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaNativeChatMessageListenerTest {
    @Test
    void relaysPersistedNativeMessageToWebSocketPublisher() {
        RealtimeMessagePublisher publisher = mock(RealtimeMessagePublisher.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        KafkaNativeChatMessageListener listener = new KafkaNativeChatMessageListener(
                new ObjectMapper(), publisher);

        listener.consume(new ConsumerRecord<>(KafkaTopics.NATIVE_MESSAGE_CREATED, 0, 0,
                "chat-session-7", Map.of("messageId", "m-1", "sessionId", 7L,
                        "sentById", 42L, "messageContent", "hello")), acknowledgment);

        verify(publisher).publishChatMessage(org.mockito.ArgumentMatchers.any(MessageCreatedEvent.class));
        verify(acknowledgment).acknowledge();
    }
}
