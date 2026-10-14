package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.swiftapp_backend.websocketservice.realtime.application.port.LocationEventPublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.event.LocationUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLocationEventPublisher implements LocationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishLocationUpdate(LocationUpdateEvent event) {
        publish(
                KafkaTopics.LOCATION_UPDATE,
                locationUpdateKey(event),
                event
        );
    }

    public CompletableFuture<SendResult<String, Object>> publish(String topic, String key, Object event) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Kafka topic must not be blank");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Kafka key must not be blank");
        }
        if (event == null) {
            throw new IllegalArgumentException("Kafka event must not be null");
        }

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {

            if (ex != null) {
                log.error(
                        "Kafka publish failed. Topic={}, Key={}",
                        topic,
                        key,
                        ex
                );
                return;
            }

            log.info(
                    "Kafka publish success. Topic={}, Key={}, Partition={}, Offset={}",
                    result.getRecordMetadata().topic(),
                    key,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });

        return future;
    }

    private String locationUpdateKey(LocationUpdateEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Location update event must not be null");
        }
        if (event.getRideId() != null) {
            return "ride-" + event.getRideId();
        }
        if (event.getDriverId() != null) {
            return "driver-" + event.getDriverId();
        }
        throw new IllegalArgumentException("Location update must contain either rideId or driverId");
    }
}
