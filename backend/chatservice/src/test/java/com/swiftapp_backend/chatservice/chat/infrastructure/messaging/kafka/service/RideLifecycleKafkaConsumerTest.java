package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.chatservice.chat.application.service.ChatService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RideLifecycleKafkaConsumerTest {
    @Test
    void opensOnAssignmentAndClosesOnCompletion() {
        ChatService chat = mock(ChatService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        RideLifecycleKafkaConsumer consumer = new RideLifecycleKafkaConsumer(new ObjectMapper(), chat);

        consumer.consume(record("ride.assigned", Map.of(
                "rideId", 101L, "riderId", 42L, "driverId", 11L,
                "rideStatus", "DRIVER_ASSIGNED")), acknowledgment);
        consumer.consume(record("ride.completed", Map.of(
                "rideId", 101L, "riderId", 42L, "driverId", 11L,
                "rideStatus", "COMPLETED")), acknowledgment);

        verify(chat).openOrUpdateSession(101L, 11L, 42L, "DRIVER_ASSIGNED", null, null);
        verify(chat).closeSessionForRide(101L, "COMPLETED", 11L, 42L);
        verify(acknowledgment, org.mockito.Mockito.times(2)).acknowledge();
    }

    @Test
    void acceptsTheAdditionalFieldsPublishedByRiderService() {
        ChatService chat = mock(ChatService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        RideLifecycleKafkaConsumer consumer = new RideLifecycleKafkaConsumer(new ObjectMapper(), chat);

        consumer.consume(record("ride.assigned", Map.of(
                "rideId", 101L, "riderId", 42L, "driverId", 11L,
                "rideStatus", "DRIVER_ASSIGNED", "vehicleId", 9L,
                "acceptedAt", "2026-10-10T01:00:00")), acknowledgment);
        consumer.consume(record("ride.completed", Map.of(
                "rideId", 101L, "riderId", 42L, "driverId", 11L,
                "rideStatus", "COMPLETED", "vehicleId", 9L,
                "completedAt", "2026-10-10T01:15:00", "actualDistance", 4.5)), acknowledgment);
        consumer.consume(record("ride.cancelled", Map.of(
                "rideId", 102L, "riderId", 42L, "driverId", 11L,
                "rideStatus", "CANCELLED", "vehicleId", 9L,
                "cancelledBy", 42L, "cancellationReason", "test cleanup")), acknowledgment);

        verify(chat).openOrUpdateSession(101L, 11L, 42L, "DRIVER_ASSIGNED", null, null);
        verify(chat).closeSessionForRide(101L, "COMPLETED", 11L, 42L);
        verify(chat).closeSessionForRide(102L, "CANCELLED", 11L, 42L);
        verify(acknowledgment, org.mockito.Mockito.times(3)).acknowledge();
    }

    private ConsumerRecord<String, Object> record(String topic, Object value) {
        return new ConsumerRecord<>(topic, 0, 0L, "101", value);
    }
}
