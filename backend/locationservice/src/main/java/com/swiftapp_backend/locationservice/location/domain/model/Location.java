package com.swiftapp_backend.locationservice.location.domain.model;

import com.swiftapp_backend.locationservice.location.domain.enums.Direction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private Long id;
    private Long driverId;
    private Long rideId;
    private Long riderId;
    private double latitude;
    private double longitude;
    private String nearByPlace;
    private Direction direction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
