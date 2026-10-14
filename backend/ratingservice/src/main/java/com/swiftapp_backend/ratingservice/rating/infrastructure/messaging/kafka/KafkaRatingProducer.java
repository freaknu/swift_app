package com.swiftapp_backend.ratingservice.rating.infrastructure.messaging.kafka;

import com.swiftapp_backend.ratingservice.rating.application.port.RatingEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaRatingProducer implements RatingEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishRatingCreated(Object event) {
        try {
            kafkaTemplate.send("rating.created", event);
            kafkaTemplate.send("rating-created", event);
            log.info("Published rating.created event");
        } catch (Exception e) {
            log.error("Error publishing rating created event", e);
        }
    }

    @Async
    public void publishRatingUpdated(Object event) {
        try {
            kafkaTemplate.send("rating.updated", event);
            kafkaTemplate.send("rating-updated", event);
            log.info("Published rating.updated event");
        } catch (Exception e) {
            log.error("Error publishing rating updated event", e);
        }
    }

    @Async
    public void publishRatingDeleted(Object event) {
        try {
            kafkaTemplate.send("rating.deleted", event);
            kafkaTemplate.send("rating-deleted", event);
            log.info("Published rating.deleted event");
        } catch (Exception e) {
            log.error("Error publishing rating deleted event", e);
        }
    }
}
