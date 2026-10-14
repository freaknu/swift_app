package com.swiftapp_backend.locationservice.location.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.locationservice.location.application.port.LocationEventPublisher;
import com.swiftapp_backend.locationservice.location.application.service.DriverMatchService;
import com.swiftapp_backend.locationservice.location.application.service.LocationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaLocationEventListenerTest {
    @Test
    void completedRideClearsItsLivePositionCache() {
        LocationService locations = mock(LocationService.class);
        KafkaLocationEventListener listener = new KafkaLocationEventListener(
                new ObjectMapper(), locations, mock(LocationEventPublisher.class),
                mock(DriverMatchService.class));
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        listener.consume(new ConsumerRecord<>("ride-completed", 0, 1L, "7",
                Map.of("rideId", 7)), acknowledgment);

        verify(locations).completeRide(7L);
        verify(acknowledgment).acknowledge();
    }
}
