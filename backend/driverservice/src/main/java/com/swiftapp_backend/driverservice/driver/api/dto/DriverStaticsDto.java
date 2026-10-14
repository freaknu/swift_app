package com.swiftapp_backend.driverservice.driver.api.dto;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverStaticsDto {
    private Integer completedTrips;
    private Integer cancelledTrips;
    private double averageRating;
    private double acceptanceRate;
    private double completionRate;
    private double lifetimeDistance;
    private double lifetimeEarning;
    private DriverResponseDto driverData;
}
