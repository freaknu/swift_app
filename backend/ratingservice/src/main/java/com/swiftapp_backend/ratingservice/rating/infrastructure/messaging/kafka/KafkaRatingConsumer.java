package com.swiftapp_backend.ratingservice.rating.infrastructure.messaging.kafka;

import com.swiftapp_backend.ratingservice.rating.application.service.RatingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaRatingConsumer {
    private final RatingEventService ratingEventService;

    @KafkaListener(
            topics = {"ride.completed", "ride-completed"},
            groupId = "rating-service"
    )
    public void handleRideCompleted(ConsumerRecord<String, Map<String, Object>> record,
                                    Acknowledgment acknowledgment) {
        try {
            log.info("Received ride completed event for rating initialization");
            Map<String, Object> data = record.value();
            
            Long rideId = data.get("rideId") != null ? ((Number) data.get("rideId")).longValue() : null;
            Long driverId = data.get("driverId") != null ? ((Number) data.get("driverId")).longValue() : null;
            Long riderId = data.get("riderId") != null ? ((Number) data.get("riderId")).longValue() : null;
            
            ratingEventService.handleRideCompleted(rideId, driverId, riderId);
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing ride completed event in rating service", e);
        }
    }

    @KafkaListener(
            topics = {"driver.created", "driver-created"},
            groupId = "rating-service"
    )
    public void handleDriverCreated(ConsumerRecord<String, Map<String, Object>> record,
                                    Acknowledgment acknowledgment) {
        try {
            log.info("Received driver created event");
            Map<String, Object> data = record.value();
            
            Long driverId = data.get("driverId") != null ? ((Number) data.get("driverId")).longValue() : null;
            
            ratingEventService.handleDriverCreated(driverId);
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing driver created event in rating service", e);
        }
    }

    @KafkaListener(
            topics = {"user.created", "user-created"},
            groupId = "rating-service"
    )
    public void handleUserCreated(ConsumerRecord<String, Map<String, Object>> record,
                                  Acknowledgment acknowledgment) {
        try {
            log.info("Received user created event");
            Map<String, Object> data = record.value();
            
            Long userId = data.get("userId") != null ? ((Number) data.get("userId")).longValue() : null;
            
            ratingEventService.handleUserCreated(userId);
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing user created event in rating service", e);
        }
    }
}
