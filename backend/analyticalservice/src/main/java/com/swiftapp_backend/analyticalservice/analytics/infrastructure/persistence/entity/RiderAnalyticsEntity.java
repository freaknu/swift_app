package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rider_analytics",indexes = {
        @Index(name = "rider_analytics_user_idx",columnList = "user_id"),
        @Index(name = "rider_analytics_auth_user_idx",columnList = "auth_user_id"),
        @Index(name = "rider_analytics_rider_idx",columnList = "rider_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiderAnalyticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAnalyticsEntity user;

    private Long authUserId;
    private Long riderId;

    private Long totalBookings = 0L;

    private Long completedTrips = 0L;

    private Long cancelledTrips = 0L;

    private Double totalSpent = 0.0;

    private Double averageRideCost = 0.0;

    private Double averageDriverRating = 0.0;

    private Long totalDistanceTravelledKm = 0L;

    private Long totalRideDurationMinutes = 0L;

    private Long couponUsed = 0L;

    private Long referralsMade = 0L;

    private Long favouriteDrivers = 0L;

    private Long airportTrips = 0L;

    private Long outstationTrips = 0L;

    private Long premiumTrips = 0L;

    private LocalDateTime lastRideBookedAt;

    private LocalDateTime lastRideCompletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
