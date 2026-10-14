package com.swiftapp_backend.driverservice.driver.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.application.service.DriverStaticsService;
import com.swiftapp_backend.driverservice.driver.application.service.DriverOfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaRideConsumerService {

    private final ObjectMapper objectMapper;
    private final DriverStaticsService driverStaticsService;
    private final DriverOfferService driverOfferService;

    @KafkaListener(
            containerFactory = "kafkaListenerContainerFactory",
            topics = {
                    "ride.completed",
                    "ride.started",
                    "ride.cancelled",
                    "payment.completed",
                    "rating.updated"
            },
            groupId = "driver-service"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        String topic = record.topic();
        Object message = record.value();
        log.debug("DriverService consumed topic {}", topic);

        try {
            if (message instanceof Map<?, ?> map) {
                Long driverId = extractLong(map.get("driverId"));
                if (driverId != null) {
                    switch (topic) {
                        case "ride.completed", "ride-completed" -> {
                            Long rideId = extractLong(map.get("rideId"));
                            if (rideId == null) rideId = extractLong(record.key());
                            if (rideId == null) throw new IllegalArgumentException("Completed ride event has no rideId");
                            driverOfferService.clearActiveRide(driverId, rideId);
                            double fare = extractDouble(map.get("totalFare"), 0.0);
                            double distance = extractDouble(map.get("actualDistance"), 0.0);
                            driverStaticsService.recordRideCompleted(rideId, driverId, fare, distance);
                        }
                        case "ride.cancelled", "ride-cancelled" -> {
                            Long rideId = extractLong(map.get("rideId"));
                            if (rideId == null) rideId = extractLong(record.key());
                            if (rideId == null) throw new IllegalArgumentException("Cancelled ride event has no rideId");
                            driverOfferService.clearActiveRide(driverId, rideId);
                            driverStaticsService.recordRideCancelled(rideId, driverId);
                        }
                        case "rating.updated" -> {
                            double rating = extractDouble(map.get("rating"), 5.0);
                            driverStaticsService.recordRatingUpdated(driverId, rating);
                        }
                        case "payment.completed" -> {
                            log.debug("Payment completed for driver {}", driverId);
                        }
                        default -> log.debug("Unhandled event on topic: {}", topic);
                    }
                }
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing driver kafka event", e);
            throw new IllegalStateException("Driver Kafka event processing failed", e);
        }
    }

    private Long extractLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s && !s.isBlank()) {
            try { return Long.valueOf(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private double extractDouble(Object val, double defaultValue) {
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s && !s.isBlank()) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
