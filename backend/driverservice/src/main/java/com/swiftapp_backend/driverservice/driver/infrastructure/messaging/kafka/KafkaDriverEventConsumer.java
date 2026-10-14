package com.swiftapp_backend.driverservice.driver.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.application.service.DriverRegistrationService;
import com.swiftapp_backend.driverservice.driver.infrastructure.messaging.kafka.event.DriverCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaDriverEventConsumer {
    private final ObjectMapper objectMapper;
    private final DriverRegistrationService driverRegistrationService;

    @KafkaListener(
            topics = {
                    "driver-created-topic"
            },
            groupId = "driver-service",
            containerFactory = "plainJsonKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            DriverCreatedEvent event =
                    objectMapper.readValue(record.value(), DriverCreatedEvent.class);
            log.info("driver account created event received: {}", event);
            driverRegistrationService.createDriverAccount(event.getAuthUserId(),
                    event.getName(), event.getEmail(), event.getPhoneNumber());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("issue while consuming driver account", e);
            throw new IllegalStateException("Driver account event processing failed", e);
        }
    }
}
