package com.swiftapp_backend.riderservice.ride.application.mapper;

import com.swiftapp_backend.riderservice.ride.api.dto.location.RecentLocationDto;
import com.swiftapp_backend.riderservice.ride.domain.model.RecentLocation;
import org.springframework.stereotype.Component;

@Component
public class RecentLocationMapper {
    public RecentLocationDto toDto(RecentLocation location) {
        if (location == null) {
            return null;
        }
        return RecentLocationDto.builder()
                .locationId(location.locationId())
                .destinationLatitude(location.destinationLatitude())
                .destinationLongitude(location.destinationLongitude())
                .message(location.message())
                .rideDate(location.rideDate())
                .build();
    }
}
