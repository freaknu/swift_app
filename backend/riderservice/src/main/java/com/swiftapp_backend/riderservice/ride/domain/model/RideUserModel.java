package com.swiftapp_backend.riderservice.ride.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class RideUserModel {
    private Long id;
    private Long authUserId;
    private String name;
    private String phoneNumber;
    private Boolean isActive;
    private RideModel ride;
    private LocalDateTime rideCreatedAt;
    private LocalDateTime rideUpdatedAt;
}
