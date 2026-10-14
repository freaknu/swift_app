package com.swiftapp_backend.driverservice.driver.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverStatisticsModel {
    private Long id;
    private Integer completedTrips;
    private Integer cancelledTrips;
    private double averageRating;
    private double acceptanceRate;
    private double completionRate;
    private double lifetimeDistance;
    private double lifetimeEarning;

    private DriverModel driver;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
