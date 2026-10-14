package com.swiftapp_backend.matchingservice.matching.api.mapper;

import com.swiftapp_backend.matchingservice.matching.api.response.DriverResponseDto;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverLocation;

public class DriverMatchingMapper {

    public static DriverResponseDto toDto(DriverLocation dto) {
        return DriverResponseDto
                .builder()
                .driverId(dto.getDriverId())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }
}
