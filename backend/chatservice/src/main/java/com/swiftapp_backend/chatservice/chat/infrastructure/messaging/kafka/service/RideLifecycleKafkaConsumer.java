package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.chatservice.chat.application.service.ChatService;
import com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.events.RideLifecycleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/** Opens and closes chat from the authoritative ride lifecycle stream. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RideLifecycleKafkaConsumer {
    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    @KafkaListener(topics = {
            "ride.assigned", "ride-assigned", "driver.assigned", "driver-assigned",
            "driver.arrived", "driver-arrived", "ride.started", "ride-started",
            "ride.completed", "ride-completed", "ride.cancelled", "ride-cancelled"
    }, groupId = "chat-service-lifecycle", containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        try {
            RideLifecycleEvent event = objectMapper.convertValue(record.value(), RideLifecycleEvent.class);
            if (event.getRideId() == null) {
                throw new IllegalArgumentException("ride lifecycle event is missing rideId");
            }
            switch (normalTopic(record.topic())) {
                case "assigned" -> chatService.openOrUpdateSession(
                        event.getRideId(), event.getDriverId(), event.getRiderId(),
                        event.getRideStatus(), null, null);
                case "completed", "cancelled" -> chatService.closeSessionForRide(
                        event.getRideId(), event.getRideStatus() == null
                                ? normalTopic(record.topic()).toUpperCase() : event.getRideStatus(),
                        event.getDriverId(), event.getRiderId());
                case "arrived", "started" -> chatService.updateRideStatus(
                        event.getRideId(), event.getRideStatus());
                default -> log.debug("Ignoring non-chat ride topic {}", record.topic());
            }
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            // Do not acknowledge: Spring Kafka can retry a transient database
            // failure, and all operations are idempotent by ride/message key.
            log.error("Unable to apply ride lifecycle event topic={} offset={}",
                    record.topic(), record.offset(), ex);
        }
    }

    private String normalTopic(String topic) {
        return switch (topic) {
            case "ride.assigned", "ride-assigned", "driver.assigned", "driver-assigned" -> "assigned";
            case "driver.arrived", "driver-arrived" -> "arrived";
            case "ride.started", "ride-started" -> "started";
            case "ride.completed", "ride-completed" -> "completed";
            case "ride.cancelled", "ride-cancelled" -> "cancelled";
            default -> topic;
        };
    }
}
