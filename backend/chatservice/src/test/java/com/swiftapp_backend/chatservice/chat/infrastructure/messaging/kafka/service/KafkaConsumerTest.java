package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.chatservice.chat.application.port.ChatPort;
import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.ChatKafkaTopics;
import com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.events.ChatSessionCreateEvent;
import com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.events.MessageCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaConsumerTest {
    private ChatPort chatPort;
    private Acknowledgment acknowledgment;
    private KafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        chatPort = mock(ChatPort.class);
        acknowledgment = mock(Acknowledgment.class);
        consumer = new KafkaConsumer(new ObjectMapper(), chatPort);
    }

    @Test
    void savesCreatedMessagesAndAcknowledgesTheEvent() {
        MessageCreatedEvent event = messageEvent(MessageType.MESSAGE);

        consumer.consume(record(ChatKafkaTopics.MESSAGE_CREATED, event), acknowledgment);

        verify(chatPort).saveMessage(any(ChatPort.MessageRecord.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void savesRepliesAsReplyMessages() {
        MessageCreatedEvent event = messageEvent(null);

        consumer.consume(record(ChatKafkaTopics.MESSAGE_REPLY, event), acknowledgment);

        var messageCaptor = org.mockito.ArgumentCaptor.forClass(ChatPort.MessageRecord.class);
        verify(chatPort).saveMessage(messageCaptor.capture());
        assertEquals(MessageType.REPLY, messageCaptor.getValue().messageType());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void updatesMessages() {
        consumer.consume(record(ChatKafkaTopics.MESSAGE_UPDATED, messageEvent(MessageType.MESSAGE)), acknowledgment);

        verify(chatPort).updateMessage(any(ChatPort.MessageRecord.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void deletesMessages() {
        consumer.consume(record(ChatKafkaTopics.MESSAGE_DELETED, messageEvent(MessageType.MESSAGE)), acknowledgment);

        verify(chatPort).deleteMessage(42L, "message-1");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void savesCreatedChatSessions() {
        ChatSessionCreateEvent event = ChatSessionCreateEvent.builder()
                .sessionNameForDriver("driver-session")
                .sessionNameForRider("rider-session")
                .driverId(11L)
                .riderId(42L)
                .rideId(101L)
                .rideStatus("STARTED")
                .build();

        consumer.consume(record(ChatKafkaTopics.CHAT_SESSION_CREATED, event), acknowledgment);

        verify(chatPort).saveSession(new ChatPort.ChatSessionRecord(
                "driver-session", "rider-session", 11L, 42L, 101L, "STARTED"));
        verify(acknowledgment).acknowledge();
    }

    private MessageCreatedEvent messageEvent(MessageType messageType) {
        return MessageCreatedEvent.builder()
                .sentById(42L)
                .messageId("message-1")
                .messageContent("hello")
                .sessionId(101L)
                .contentType(ContentType.TEXT)
                .messageType(messageType)
                .build();
    }

    private ConsumerRecord<String, Object> record(String topic, Object value) {
        return new ConsumerRecord<>(topic, 0, 1L, "message-1", value);
    }
}
