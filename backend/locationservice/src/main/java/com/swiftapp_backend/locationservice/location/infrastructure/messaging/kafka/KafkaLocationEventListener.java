package com.swiftapp_backend.locationservice.location.infrastructure.messaging.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.locationservice.location.api.dto.event.DriverFindRequestDto;
import com.swiftapp_backend.locationservice.location.api.dto.event.LocationUpdateDto;
import com.swiftapp_backend.locationservice.location.application.port.LocationEventPublisher;
import com.swiftapp_backend.locationservice.location.application.service.DriverMatchService;
import com.swiftapp_backend.locationservice.location.application.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLocationEventListener {

    private final ObjectMapper objectMapper;
    private final LocationService locationService;
    private final LocationEventPublisher eventPublisher;
    private final DriverMatchService driverMatchService;

    @KafkaListener(
            topics = {
                    "location-update-topic",
                    "ride-cancelled",
                    "match-driver-topic",
                    "ride-requested",
                    "ride-completed",
                    "driver.matching.status"
            },
            groupId = "location-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {

        try {

            Object dto = record.value();
            String topic = record.topic();
            log.info("Topic : {}", topic);
            log.info("Message : {}", dto);

            switch (topic) {
                case ("location-update-topic") :
                    saveLocation(dto);
                    break;
                case ("ride-cancelled") :
                case ("ride-completed") :
                    rideComplete(dto);
                    break;
                case ("match-driver-topic") :
                    matchDriver(dto);
                    break;
                case ("ride-requested") :
                    matchDriverFromRideRequest(dto);
                    break;
                case ("driver.matching.status") :
                    driverStatusChanged(dto);
                    break;
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error while consuming message", e);
            throw new IllegalStateException("Location service Kafka listener failed", e);
        }
    }

    private void saveLocation(Object data) {
        LocationUpdateDto location = objectMapper.convertValue(data,LocationUpdateDto.class);
        locationService.addLocation(location);
        if(location.getRideId() == null) return;
        eventPublisher.publish(
                "location-updated-topic",
                "ride-location-" + location.getRideId(),
                location
        );
    }

    private void driverStatusChanged(Object data) {
        Map<?, ?> payload = objectMapper.convertValue(data, Map.class);
        Object rawDriverId = payload.get("driverId");
        if (rawDriverId != null && !"ACTIVE".equals(String.valueOf(payload.get("status")))) {
            locationService.driverBecameUnavailable(Long.valueOf(rawDriverId.toString()));
        }
    }


    private void rideComplete(Object data) {
        Long rideId = extractRideId(data);
        if (rideId == null) {
            log.warn("Unable to complete ride location cleanup because rideId is missing. Payload={}", data);
            return;
        }
        locationService.completeRide(rideId);
    }


    // match the driver and broadcast
    private void matchDriver(Object data) {
        DriverFindRequestDto driverFindRequestDto = objectMapper.convertValue(data,DriverFindRequestDto.class);
        driverMatchService.matchDriver(driverFindRequestDto);
    }

    private void matchDriverFromRideRequest(Object data) {
        Map<String, Object> payload = objectMapper.convertValue(
                data,
                new TypeReference<>() {}
        );

        Double latitude = extractDouble(payload.get("pickupLatitude"));
        Double longitude = extractDouble(payload.get("pickupLongitude"));
        if (latitude == null || longitude == null) {
            log.warn("Unable to match driver because pickup coordinates are missing. Payload={}", data);
            return;
        }

        driverMatchService.matchDriver(new DriverFindRequestDto(latitude, longitude));
    }

    private Double extractDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long extractRideId(Object data) {
        if (data instanceof Number number) {
            return number.longValue();
        }
        if (data instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.convertValue(
                    data,
                    new TypeReference<>() {}
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        Object rideId = payload.get("rideId");
        if (rideId instanceof Number number) {
            return number.longValue();
        }
        if (rideId instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
