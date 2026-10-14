package com.swiftapp_backend.locationservice.location.api.dto.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class NearbyDriverDto {
    private Long driverId;
    private String nearByPlace;
    private double latitude;
    private double longitude;
    private String vehicleType;
    private Instant updatedAt;
}
