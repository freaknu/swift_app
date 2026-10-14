package com.swiftapp_backend.analyticalservice.analytics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverAnalyticsResponseDto {
    private Long driverId;
    private Integer totalRides;
    private Integer completedRides;
    private Integer cancelledRides;
    private Double totalEarnings;
    private Double averageRating;
    private Double acceptanceRate;
    private Double cancellationRate;
    private Long totalOnlineHours;
    private Double averageRideDistance;
}
