package com.swiftapp_backend.authservice.authentication.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.authservice.authentication.application.service.AuthService;
import com.swiftapp_backend.authservice.authentication.domain.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final AuthService authService;

    @KafkaListener(
            topics = {"user.deleted", "user-deleted-topic"},
            groupId = "auth-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("AuthService received message on topic {}: {}", record.topic(), record.value());
            UserDeletedEvent event = objectMapper.convertValue(record.value(), UserDeletedEvent.class);
            authService.handleUserDeleted(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing user.deleted event", e);
        }
    }
}
