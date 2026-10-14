package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.swiftapp_backend.websocketservice.realtime.domain.enums.ContentType;
import com.swiftapp_backend.websocketservice.realtime.domain.enums.MessageType;
import com.swiftapp_backend.websocketservice.realtime.domain.event.MessageCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaChatEventPublisherTest {

    @Test
    void publishesCreatedMessageKeyedBySession() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaChatEventPublisher publisher = new KafkaChatEventPublisher(kafkaTemplate);
        MessageCreatedEvent event = chatEvent();
        when(kafkaTemplate.send(KafkaTopics.MESSAGE_CREATED, "chat-session-101", event))
                .thenReturn(new CompletableFuture<>());

        publisher.sendMessageCreated(event);

        verify(kafkaTemplate).send("message.created", "chat-session-101", event);
    }

    @Test
    void publishesReplyUpdateAndDeleteOnTheirTopics() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaChatEventPublisher publisher = new KafkaChatEventPublisher(kafkaTemplate);
        MessageCreatedEvent event = chatEvent();
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(event))).thenReturn(future);

        publisher.sendMessageReply(event);
        publisher.sendMessageUpdated(event);
        publisher.sendMessageDelete(event);

        verify(kafkaTemplate).send("message.reply", "chat-session-101", event);
        verify(kafkaTemplate).send("message.updated", "chat-session-101", event);
        verify(kafkaTemplate).send("message.deleted", "chat-session-101", event);
    }

    @Test
    void fallsBackToMessageIdAsKeyWithoutSession() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaChatEventPublisher publisher = new KafkaChatEventPublisher(kafkaTemplate);
        MessageCreatedEvent event = chatEvent();
        event.setSessionId(null);
        when(kafkaTemplate.send(KafkaTopics.MESSAGE_CREATED, "msg-1", event))
                .thenReturn(new CompletableFuture<>());

        publisher.sendMessageCreated(event);

        verify(kafkaTemplate).send("message.created", "msg-1", event);
    }

    private MessageCreatedEvent chatEvent() {
        return MessageCreatedEvent.builder()
                .messageId("msg-1")
                .sentById(42L)
                .messageContent("hello")
                .sessionId(101L)
                .contentType(ContentType.TEXT)
                .messageType(MessageType.MESSAGE)
                .build();
    }
}
