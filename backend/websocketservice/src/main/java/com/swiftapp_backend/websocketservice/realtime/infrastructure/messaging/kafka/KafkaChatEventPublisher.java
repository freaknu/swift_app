package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.swiftapp_backend.websocketservice.realtime.application.port.ChatEventPublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.event.ChatSessionCreateEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.MessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Durable path for chat: publishes to Kafka so chatservice can persist.
 * No DB access here — this service stays fast.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaChatEventPublisher implements ChatEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void sendMessageCreated(MessageCreatedEvent event) {
        send(KafkaTopics.MESSAGE_CREATED, event);
    }

    @Override
    public void sendMessageReply(MessageCreatedEvent event) {
        send(KafkaTopics.MESSAGE_REPLY, event);
    }

    @Override
    public void sendMessageUpdated(MessageCreatedEvent event) {
        send(KafkaTopics.MESSAGE_UPDATED, event);
    }

    @Override
    public void sendMessageDelete(MessageCreatedEvent event) {
        send(KafkaTopics.MESSAGE_DELETED, event);
    }

    @Override
    public void sendSessionCreated(ChatSessionCreateEvent event) {
        String key = event.getRideId() != null
                ? "chat-session-create-" + event.getRideId()
                : "chat-session-create";
        kafkaTemplate.send(KafkaTopics.CHAT_SESSION_CREATED, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Chat session Kafka publish failed topic={} key={}", KafkaTopics.CHAT_SESSION_CREATED, key, ex);
            } else {
                log.debug("Chat session Kafka publish ok topic={} key={} offset={}",
                        KafkaTopics.CHAT_SESSION_CREATED, key, result.getRecordMetadata().offset());
            }
        });
    }

    private void send(String topic, MessageCreatedEvent event) {
        // Key by session so one chat session's events stay ordered on one partition.
        String key = event.getSessionId() != null
                ? "chat-session-" + event.getSessionId()
                : event.getMessageId();
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Chat Kafka publish failed topic={} key={}", topic, key, ex);
            } else {
                log.debug("Chat Kafka publish ok topic={} key={} offset={}",
                        topic, key, result.getRecordMetadata().offset());
            }
        });
    }
}
