package com.swiftapp_backend.websocketservice.realtime.api.dto;

import com.swiftapp_backend.websocketservice.realtime.domain.enums.Direction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class LocationUpdateDto {
    private Long id;
    private Long driverId;
    private Long rideId;
    private Long riderId;
    private double latitude;
    private double longitude;
    private String nearByPlace;
    private Direction direction;
}
