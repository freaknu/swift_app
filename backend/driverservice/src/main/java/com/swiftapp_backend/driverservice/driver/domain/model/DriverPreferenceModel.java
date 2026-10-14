package com.swiftapp_backend.driverservice.driver.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverPreferenceModel {
    private Long id;

    private DriverModel driver;

    private boolean autoAccept;

    private boolean acceptCash;

    private boolean acceptOnline;

    private double preferredRadius;

    private Set<String> preferredRideTypes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
