package com.swiftapp_backend.analyticalservice.analytics.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverAnalytics {
    private Long id;
    private UserAnalytics user;
    private Long authUserId;
    private Long driverId;
    private Long totalTrips;
    private Long completedTrips;
    private Long cancelledTrips;
    private Long rejectedTrips;
    private Long totalDistanceKm;
    private Long totalRideDurationMinutes;
    private Double averageRating;
    private Double acceptanceRate;
    private Double cancellationRate;
    private Double totalEarnings;
    private Double todayEarnings;
    private Double weeklyEarnings;
    private Double monthlyEarnings;
    private Long onlineHours;
    private Long peakHourTrips;
    private Long nightTrips;
    private Long airportTrips;
    private Long outstationTrips;
    private Long currentStreakDays;
    private LocalDateTime lastRideCompletedAt;
    private LocalDateTime lastOnlineAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
