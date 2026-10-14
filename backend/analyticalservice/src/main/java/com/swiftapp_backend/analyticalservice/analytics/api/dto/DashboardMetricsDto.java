package com.swiftapp_backend.analyticalservice.analytics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private long trackedDrivers;
    private long trackedRiders;
    private long totalCompletedTrips;
    private long totalCancelledTrips;
    private double totalEarnings;
    private double totalSpent;
}
