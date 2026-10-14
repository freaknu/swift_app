package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.chatservice.chat.application.port.ChatPort;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.ChatKafkaTopics;
import com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.events.ChatSessionCreateEvent;
import com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.events.MessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumer {
    private final ObjectMapper objectMapper;
    private final ChatPort chatPort;

    @KafkaListener(topics = {
            ChatKafkaTopics.MESSAGE_CREATED,
            ChatKafkaTopics.MESSAGE_UPDATED,
            ChatKafkaTopics.MESSAGE_DELETED,
            ChatKafkaTopics.MESSAGE_REPLY,
            ChatKafkaTopics.CHAT_SESSION_CREATED
    }, groupId = "chat-service", containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        String topic = record.topic();
        try {
            Object data = record.value();
            switch (topic) {
                case ChatKafkaTopics.MESSAGE_CREATED -> consumeMessageCreated(data);
                case ChatKafkaTopics.MESSAGE_REPLY -> consumeMessageReply(data);
                case ChatKafkaTopics.MESSAGE_UPDATED -> consumeMessageUpdated(data);
                case ChatKafkaTopics.MESSAGE_DELETED -> consumeMessageDeleted(data);
                case ChatKafkaTopics.CHAT_SESSION_CREATED -> consumeSessionCreated(data);
                default -> log.warn("Ignoring record from unexpected topic {}", topic);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error while consuming topic={} partition={} offset={} key={} : {}",
                    topic, record.partition(), record.offset(), record.key(), e.getMessage(), e);
        }
    }

    private void consumeMessageCreated(Object data) {
        MessageCreatedEvent message = objectMapper.convertValue(data, MessageCreatedEvent.class);
        chatPort.saveMessage(toRecord(message, message.getMessageType()));
    }

    private void consumeMessageReply(Object data) {
        MessageCreatedEvent message = objectMapper.convertValue(data, MessageCreatedEvent.class);
        chatPort.saveMessage(toRecord(message, MessageType.REPLY));
    }

    private void consumeMessageUpdated(Object data) {
        MessageCreatedEvent message = objectMapper.convertValue(data, MessageCreatedEvent.class);
        chatPort.updateMessage(toRecord(message, message.getMessageType()));
    }

    private void consumeMessageDeleted(Object data) {
        MessageCreatedEvent message = objectMapper.convertValue(data, MessageCreatedEvent.class);
        chatPort.deleteMessage(message.getSentById(), message.getMessageId());
    }

    private void consumeSessionCreated(Object data) {
        ChatSessionCreateEvent chat = objectMapper.convertValue(data, ChatSessionCreateEvent.class);
        chatPort.saveSession(new ChatPort.ChatSessionRecord(
                chat.getSessionNameForDriver(), chat.getSessionNameForRider(),
                chat.getDriverId(), chat.getRiderId(), chat.getRideId(), chat.getRideStatus()));
    }

    private ChatPort.MessageRecord toRecord(MessageCreatedEvent message, MessageType messageType) {
        return new ChatPort.MessageRecord(
                message.getSentById(),
                message.getMessageId(),
                message.getMessageContent(),
                message.getSessionId(),
                message.getMessageUrl(),
                message.getReplyOfMessageId(),
                message.getContentType(),
                messageType,
                message.getReadByIds(),
                message.isEdited(),
                message.isDeleted());
    }
}
