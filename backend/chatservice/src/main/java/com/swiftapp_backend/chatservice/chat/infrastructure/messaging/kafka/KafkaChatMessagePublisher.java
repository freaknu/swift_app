package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka;

import com.swiftapp_backend.chatservice.chat.application.port.ChatMessagePublisher;
import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import com.swiftapp_backend.chatservice.chat.domain.model.ChatSession;
import com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.events.MessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Publishes messages already committed by the native chat API to WebSocket. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaChatMessagePublisher implements ChatMessagePublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(Message message, ChatSession session) {
        Long recipientId = message.getSentById() != null
                && message.getSentById().equals(session.getRiderId())
                ? session.getDriverId() : session.getRiderId();
        String recipientRole = message.getSentById() != null
                && message.getSentById().equals(session.getRiderId())
                ? "DRIVER" : "RIDER";
        MessageCreatedEvent event = MessageCreatedEvent.builder()
                .sentById(message.getSentById())
                .messageId(message.getMessageId())
                .messageContent(message.getMessageContent())
                .sessionId(message.getSessionId())
                .messageUrl(message.getMessageUrl())
                .replyOfMessageId(message.getReplyOfMessageId())
                .contentType(message.getContentType())
                .messageType(message.getMessageType())
                .readByIds(message.getReadByIds())
                .rideId(session.getRideId())
                .riderId(session.getRiderId())
                .driverId(session.getDriverId())
                .recipientId(recipientId)
                .recipientRole(recipientRole)
                .build();
        event.setEdited(message.isEdited());
        event.setDeleted(message.isDeleted());
        String key = message.getSessionId() == null
                ? message.getMessageId()
                : "chat-session-" + message.getSessionId();
        kafkaTemplate.send(ChatKafkaTopics.NATIVE_MESSAGE_CREATED, key, event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Native chat WebSocket publish failed for message {}", message.getMessageId(), error);
                    }
                });
    }
}
