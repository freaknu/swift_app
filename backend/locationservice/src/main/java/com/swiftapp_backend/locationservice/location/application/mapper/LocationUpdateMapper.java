package com.swiftapp_backend.locationservice.location.application.mapper;

import com.swiftapp_backend.locationservice.location.api.dto.event.LocationUpdateDto;
import com.swiftapp_backend.locationservice.location.domain.model.Location;
import org.springframework.stereotype.Component;
import java.time.ZoneId;

@Component
public class LocationUpdateMapper {
    public LocationUpdateDto toDto(Location data) {
        return LocationUpdateDto
                .builder()
                .id(data.getId())
                .rideId(data.getRideId())
                .riderId(data.getRiderId())
                .nearByPlace(data.getNearByPlace())
                .driverId(data.getDriverId())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .direction(data.getDirection())
                .eventTime(data.getCreatedAt() == null ? null
                        : data.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }

    public Location toModel(LocationUpdateDto data) {
        return Location
                .builder()
                .nearByPlace(data.getNearByPlace())
                .direction(data.getDirection())
                .rideId(data.getRideId())
                .riderId(data.getRiderId())
                .driverId(data.getDriverId())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .build();
    }
}
