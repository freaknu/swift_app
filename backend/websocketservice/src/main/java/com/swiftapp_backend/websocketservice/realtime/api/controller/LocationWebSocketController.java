package com.swiftapp_backend.websocketservice.realtime.api.controller;

import com.swiftapp_backend.websocketservice.realtime.api.dto.LocationUpdateDto;
import com.swiftapp_backend.websocketservice.realtime.application.service.LiveLocationService;
import com.swiftapp_backend.websocketservice.realtime.domain.model.LocationUpdate;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.config.DriverIdentityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LocationWebSocketController {
    private final LiveLocationService locationService;
    private final DriverIdentityClient driverIdentityClient;

    @MessageMapping("/location")
    public void consumeLocation(SimpMessageHeaderAccessor headers,
                                LocationUpdateDto data) {
        log.info("The coming data is {}", data);
        // Native STOMP headers are client controlled; only CONNECT's verified
        // JWT identity stored in the session can authorize location updates.
        Long authUserId = sessionUserId(headers);
        if (authUserId == null || data.getDriverId() == null
                || !driverIdentityClient.belongsTo(authUserId, data.getDriverId())) {
            throw new MessagingException("Driver location is not authorized");
        }
        locationService.consumeLocation(toDomain(data));
    }

    private Long sessionUserId(SimpMessageHeaderAccessor headers) {
        Map<String, Object> attributes = headers == null ? null : headers.getSessionAttributes();
        Object value = attributes == null ? null : attributes.get("swift.authUserId");
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }



    private LocationUpdate toDomain(LocationUpdateDto data) {
       return LocationUpdate.builder()
                .id(data.getId())
                .driverId(data.getDriverId())
                .rideId(data.getRideId())
                .riderId(data.getRiderId())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .nearByPlace(data.getNearByPlace())
                .direction(data.getDirection())
                .build();
    }
}
