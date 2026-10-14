package com.swiftapp_backend.websocketservice.realtime.application.mapper;

import com.swiftapp_backend.websocketservice.realtime.domain.event.LocationUpdateEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.model.LocationUpdate;
import org.springframework.stereotype.Component;

@Component
public class LocationUpdateMapper {

    public LocationUpdateEvent toEvent(LocationUpdate location) {
        return LocationUpdateEvent.builder()
                .id(location.getId())
                .driverId(location.getDriverId())
                .rideId(location.getRideId())
                .riderId(location.getRiderId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .nearByPlace(location.getNearByPlace())
                .direction(location.getDirection())
                .build();
    }
}
