package com.swiftapp_backend.riderservice.ride.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RideDriverModel {
    private Long id;
    private Long driverId;
    private Long authUserId;
    private String driverName;
    private String driverEmail;
    private String driverPhone;
    private String driverGender;
    private String driverProfileImage;
    private Boolean isActive;

    private RideModel ride;

    private LocalDateTime rideDriverCreatedAt;
    private LocalDateTime rideDriverUpdatedAt;
}
