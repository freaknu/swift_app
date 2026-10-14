package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.swiftapp_backend.websocketservice.realtime.domain.event.DriverArrivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Publishes server-detected lifecycle transitions back to rider-service. */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaRideEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishDriverArrivalDetected(DriverArrivedEvent event) {
        if (event == null || event.getRideId() == null || event.getDriverId() == null) {
            throw new IllegalArgumentException("Driver arrival event requires rideId and driverId");
        }
        kafkaTemplate.send(
                KafkaTopics.DRIVER_ARRIVAL_DETECTED,
                event.getRideId().toString(),
                event
        ).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Could not publish automatic arrival for ride {}",
                        event.getRideId(), error);
            } else {
                log.debug("Published automatic arrival for ride {}", event.getRideId());
            }
        });
    }
}
