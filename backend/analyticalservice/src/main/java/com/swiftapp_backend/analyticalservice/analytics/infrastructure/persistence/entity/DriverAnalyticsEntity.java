package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "driver_analytics",indexes = {
        @Index(name = "driver_analytics_user_idx",columnList = "user_id"),
        @Index(name = "driver_analytics_auth_user_idx",columnList = "auth_user_id"),
        @Index(name = "driver_analytics_driver_idx",columnList = "driver_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverAnalyticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAnalyticsEntity user;
    private Long authUserId;
    private Long driverId;
    private Long totalTrips = 0L;
    private Long completedTrips = 0L;
    private Long cancelledTrips = 0L;
    private Long rejectedTrips = 0L;
    private Long totalDistanceKm = 0L;
    private Long totalRideDurationMinutes = 0L;
    private Double averageRating = 0.0;
    private Double acceptanceRate = 0.0;
    private Double cancellationRate = 0.0;
    private Double totalEarnings = 0.0;
    private Double todayEarnings = 0.0;
    private Double weeklyEarnings = 0.0;
    private Double monthlyEarnings = 0.0;
    private Long onlineHours = 0L;
    private Long peakHourTrips = 0L;
    private Long nightTrips = 0L;
    private Long airportTrips = 0L;
    private Long outstationTrips = 0L;
    private Long currentStreakDays = 0L;
    private LocalDateTime lastRideCompletedAt;
    private LocalDateTime lastOnlineAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
