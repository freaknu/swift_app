package com.swiftapp_backend.driverservice.driver.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.application.service.DriverOfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class DriverOfferKafkaConsumer {

    private final DriverOfferService offerService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "ride.offer.broadcast",
            groupId = "driver-service",
            containerFactory = "plainJsonKafkaListenerContainerFactory"
    )
    public void consumeBroadcast(ConsumerRecord<String, String> record,
                                 Acknowledgment acknowledgment) {
        try {
            Map<String, Object> payload = toMap(record.value());
            Long rideId = toLong(payload.get("rideId"));
            Long riderId = toLong(payload.get("riderId"));
            String expiresAt = payload.get("expiresAt") != null
                    ? String.valueOf(payload.get("expiresAt"))
                    : null;
            List<Map<String, Object>> targets = toTargetList(payload.get("targets"));

            if (rideId == null || targets.isEmpty()) {
                log.warn("Ignoring ride.offer.broadcast without rideId/targets: {}",
                        record.value());
                acknowledgment.acknowledge();
                return;
            }

            int staged = 0;
            for (Map<String, Object> target : targets) {
                Long driverId = toLong(target.get("driverId"));
                if (driverId == null) {
                    continue;
                }
                offerService.savePendingOffer(
                        driverId,
                        rideId,
                        riderId,
                        toDouble(target.get("distanceKm")),
                        expiresAt,
                        toNullableDouble(payload.get("pickupLatitude")),
                        toNullableDouble(payload.get("pickupLongitude")),
                        toNullableDouble(payload.get("destinationLatitude")),
                        toNullableDouble(payload.get("destinationLongitude")),
                        toNullableDouble(payload.get("estimatedDistance")),
                        toLong(payload.get("estimatedDuration")),
                        toBigDecimal(payload.get("totalFare")),
                        payload.get("currency") == null ? null :
                                payload.get("currency").toString(),
                        payload.get("vehicleType") == null ? null :
                                payload.get("vehicleType").toString());
                staged++;
            }
            acknowledgment.acknowledge();
            log.info("Staged broadcast offers for ride {} ({} drivers)", rideId, staged);
        } catch (Exception e) {
            // A malformed message will never parse on redelivery; ack it so it
            // cannot wedge the partition. The matching-service offer TTL is the
            // backstop for genuinely lost offers.
            log.error("Failed to stage ride.offer.broadcast, skipping: {}", e.getMessage());
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(
            topics = "ride.offer.withdrawn",
            groupId = "driver-service",
            containerFactory = "plainJsonKafkaListenerContainerFactory"
    )
    public void consumeWithdrawn(ConsumerRecord<String, String> record,
                                 Acknowledgment acknowledgment) {
        try {
            Map<String, Object> payload = toMap(record.value());
            Long rideId = toLong(payload.get("rideId"));
            List<Object> driverIds = toIdList(payload.get("driverIds"));

            if (rideId == null || driverIds.isEmpty()) {
                log.warn("Ignoring ride.offer.withdrawn without rideId/driverIds: {}",
                        record.value());
                acknowledgment.acknowledge();
                return;
            }

            for (Object rawId : driverIds) {
                Long driverId = toLong(rawId);
                if (driverId != null) {
                    offerService.withdrawOffer(driverId, rideId);
                }
            }
            acknowledgment.acknowledge();
            log.info("Withdrew staged offers for ride {} ({} drivers, reason={})",
                    rideId, driverIds.size(), payload.get("reason"));
        } catch (Exception e) {
            log.error("Failed to process ride.offer.withdrawn, skipping: {}", e.getMessage());
            acknowledgment.acknowledge();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object value) throws Exception {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        if (value instanceof String json) {
            return objectMapper.readValue(json, Map.class);
        }
        // Fallback: let Jackson convert whatever the deserializer produced.
        return objectMapper.convertValue(value, Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toTargetList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Object> toIdList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double toNullableDouble(Object value) {
        return value == null ? null : toDouble(value);
    }

    private java.math.BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        try {
            return new java.math.BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
