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
public class RiderAnalytics {
    private Long id;
    private UserAnalytics user;
    private Long authUserId;
    private Long riderId;
    private Long totalBookings;
    private Long completedTrips;
    private Long cancelledTrips;
    private Double totalSpent;
    private Double averageRideCost;
    private Double averageDriverRating;
    private Long totalDistanceTravelledKm;
    private Long totalRideDurationMinutes;
    private Long couponUsed;
    private Long referralsMade;
    private Long favouriteDrivers;
    private Long airportTrips;
    private Long outstationTrips;
    private Long premiumTrips;
    private LocalDateTime lastRideBookedAt;
    private LocalDateTime lastRideCompletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
