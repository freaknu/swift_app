package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.swiftapp_backend.websocketservice.realtime.domain.event.LocationUpdateEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaLocationEventPublisherTest {

    @Test
    void publishesRideLocationWithRideKeyOnExistingTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaLocationEventPublisher publisher = new KafkaLocationEventPublisher(kafkaTemplate);
        LocationUpdateEvent event = LocationUpdateEvent.builder()
                .rideId(101L)
                .driverId(11L)
                .build();
        CompletableFuture<SendResult<String, Object>> result = new CompletableFuture<>();
        when(kafkaTemplate.send(KafkaTopics.LOCATION_UPDATE, "ride-101", event)).thenReturn(result);

        publisher.publishLocationUpdate(event);

        verify(kafkaTemplate).send("location-update-topic", "ride-101", event);
    }

    @Test
    void publishesAvailableDriverLocationWithDriverKeyOnExistingTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaLocationEventPublisher publisher = new KafkaLocationEventPublisher(kafkaTemplate);
        LocationUpdateEvent event = LocationUpdateEvent.builder()
                .driverId(11L)
                .build();
        CompletableFuture<SendResult<String, Object>> result = new CompletableFuture<>();
        when(kafkaTemplate.send(KafkaTopics.LOCATION_UPDATE, "driver-11", event)).thenReturn(result);

        publisher.publishLocationUpdate(event);

        verify(kafkaTemplate).send("location-update-topic", "driver-11", event);
    }
}
