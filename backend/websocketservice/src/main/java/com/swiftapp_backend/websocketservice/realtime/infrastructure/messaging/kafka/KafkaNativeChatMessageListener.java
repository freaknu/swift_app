package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.websocketservice.realtime.application.port.RealtimeMessagePublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.event.MessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Relays messages persisted by chatservice to the existing Redis/STOMP fanout. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNativeChatMessageListener {
    private final ObjectMapper objectMapper;
    private final RealtimeMessagePublisher realtimeMessagePublisher;
    private final Map<String, Long> relayedMessages = new ConcurrentHashMap<>();

    @KafkaListener(topics = KafkaTopics.NATIVE_MESSAGE_CREATED,
            groupId = "websocket-chat-rest", containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        try {
            MessageCreatedEvent event = objectMapper.convertValue(record.value(), MessageCreatedEvent.class);
            if (event.getMessageId() == null || event.getSessionId() == null) {
                throw new IllegalArgumentException("Native chat event is missing messageId/sessionId");
            }
            if (relayedMessages.putIfAbsent(event.getMessageId(), System.currentTimeMillis()) != null) {
                acknowledgment.acknowledge();
                return;
            }
            realtimeMessagePublisher.publishChatMessage(event);
            if (relayedMessages.size() > 10_000) {
                long expiry = System.currentTimeMillis() - 900_000;
                relayedMessages.entrySet().removeIf(entry -> entry.getValue() < expiry);
            }
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            try {
                MessageCreatedEvent event = objectMapper.convertValue(record.value(), MessageCreatedEvent.class);
                if (event.getMessageId() != null) relayedMessages.remove(event.getMessageId());
            } catch (RuntimeException ignored) {
                // Preserve the original conversion/relay failure.
            }
            log.error("Unable to relay native chat message at offset {}", record.offset(), ex);
        }
    }
}
