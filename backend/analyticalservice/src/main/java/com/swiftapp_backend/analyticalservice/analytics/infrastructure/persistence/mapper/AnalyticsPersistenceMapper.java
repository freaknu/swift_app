package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.mapper;

import com.swiftapp_backend.analyticalservice.analytics.domain.model.DriverAnalytics;
import com.swiftapp_backend.analyticalservice.analytics.domain.model.RiderAnalytics;
import com.swiftapp_backend.analyticalservice.analytics.domain.model.UserAnalytics;
import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.entity.DriverAnalyticsEntity;
import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.entity.RiderAnalyticsEntity;
import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.entity.UserAnalyticsEntity;

public final class AnalyticsPersistenceMapper {
    private AnalyticsPersistenceMapper() {
    }

    public static UserAnalyticsEntity toEntity(UserAnalytics source) {
        if (source == null) return null;
        return UserAnalyticsEntity.builder()
                .id(source.getId())
                .authUserId(source.getAuthUserId())
                .role(source.getRole())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build();
    }

    public static UserAnalytics toDomain(UserAnalyticsEntity source) {
        if (source == null) return null;
        return UserAnalytics.builder()
                .id(source.getId())
                .authUserId(source.getAuthUserId())
                .role(source.getRole())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build();
    }

    public static DriverAnalyticsEntity toEntity(DriverAnalytics source) {
        if (source == null) return null;
        return DriverAnalyticsEntity.builder()
                .id(source.getId()).user(toEntity(source.getUser()))
                .authUserId(source.getAuthUserId()).driverId(source.getDriverId())
                .totalTrips(source.getTotalTrips()).completedTrips(source.getCompletedTrips())
                .cancelledTrips(source.getCancelledTrips()).rejectedTrips(source.getRejectedTrips())
                .totalDistanceKm(source.getTotalDistanceKm()).totalRideDurationMinutes(source.getTotalRideDurationMinutes())
                .averageRating(source.getAverageRating()).acceptanceRate(source.getAcceptanceRate())
                .cancellationRate(source.getCancellationRate()).totalEarnings(source.getTotalEarnings())
                .todayEarnings(source.getTodayEarnings()).weeklyEarnings(source.getWeeklyEarnings())
                .monthlyEarnings(source.getMonthlyEarnings()).onlineHours(source.getOnlineHours())
                .peakHourTrips(source.getPeakHourTrips()).nightTrips(source.getNightTrips())
                .airportTrips(source.getAirportTrips()).outstationTrips(source.getOutstationTrips())
                .currentStreakDays(source.getCurrentStreakDays())
                .lastRideCompletedAt(source.getLastRideCompletedAt()).lastOnlineAt(source.getLastOnlineAt())
                .createdAt(source.getCreatedAt()).updatedAt(source.getUpdatedAt())
                .build();
    }

    public static DriverAnalytics toDomain(DriverAnalyticsEntity source) {
        if (source == null) return null;
        return DriverAnalytics.builder()
                .id(source.getId()).user(toDomain(source.getUser()))
                .authUserId(source.getAuthUserId()).driverId(source.getDriverId())
                .totalTrips(source.getTotalTrips()).completedTrips(source.getCompletedTrips())
                .cancelledTrips(source.getCancelledTrips()).rejectedTrips(source.getRejectedTrips())
                .totalDistanceKm(source.getTotalDistanceKm()).totalRideDurationMinutes(source.getTotalRideDurationMinutes())
                .averageRating(source.getAverageRating()).acceptanceRate(source.getAcceptanceRate())
                .cancellationRate(source.getCancellationRate()).totalEarnings(source.getTotalEarnings())
                .todayEarnings(source.getTodayEarnings()).weeklyEarnings(source.getWeeklyEarnings())
                .monthlyEarnings(source.getMonthlyEarnings()).onlineHours(source.getOnlineHours())
                .peakHourTrips(source.getPeakHourTrips()).nightTrips(source.getNightTrips())
                .airportTrips(source.getAirportTrips()).outstationTrips(source.getOutstationTrips())
                .currentStreakDays(source.getCurrentStreakDays())
                .lastRideCompletedAt(source.getLastRideCompletedAt()).lastOnlineAt(source.getLastOnlineAt())
                .createdAt(source.getCreatedAt()).updatedAt(source.getUpdatedAt())
                .build();
    }

    public static RiderAnalyticsEntity toEntity(RiderAnalytics source) {
        if (source == null) return null;
        return RiderAnalyticsEntity.builder()
                .id(source.getId()).user(toEntity(source.getUser()))
                .authUserId(source.getAuthUserId()).riderId(source.getRiderId())
                .totalBookings(source.getTotalBookings()).completedTrips(source.getCompletedTrips())
                .cancelledTrips(source.getCancelledTrips()).totalSpent(source.getTotalSpent())
                .averageRideCost(source.getAverageRideCost()).averageDriverRating(source.getAverageDriverRating())
                .totalDistanceTravelledKm(source.getTotalDistanceTravelledKm())
                .totalRideDurationMinutes(source.getTotalRideDurationMinutes())
                .couponUsed(source.getCouponUsed()).referralsMade(source.getReferralsMade())
                .favouriteDrivers(source.getFavouriteDrivers()).airportTrips(source.getAirportTrips())
                .outstationTrips(source.getOutstationTrips()).premiumTrips(source.getPremiumTrips())
                .lastRideBookedAt(source.getLastRideBookedAt()).lastRideCompletedAt(source.getLastRideCompletedAt())
                .createdAt(source.getCreatedAt()).updatedAt(source.getUpdatedAt())
                .build();
    }

    public static RiderAnalytics toDomain(RiderAnalyticsEntity source) {
        if (source == null) return null;
        return RiderAnalytics.builder()
                .id(source.getId()).user(toDomain(source.getUser()))
                .authUserId(source.getAuthUserId()).riderId(source.getRiderId())
                .totalBookings(source.getTotalBookings()).completedTrips(source.getCompletedTrips())
                .cancelledTrips(source.getCancelledTrips()).totalSpent(source.getTotalSpent())
                .averageRideCost(source.getAverageRideCost()).averageDriverRating(source.getAverageDriverRating())
                .totalDistanceTravelledKm(source.getTotalDistanceTravelledKm())
                .totalRideDurationMinutes(source.getTotalRideDurationMinutes())
                .couponUsed(source.getCouponUsed()).referralsMade(source.getReferralsMade())
                .favouriteDrivers(source.getFavouriteDrivers()).airportTrips(source.getAirportTrips())
                .outstationTrips(source.getOutstationTrips()).premiumTrips(source.getPremiumTrips())
                .lastRideBookedAt(source.getLastRideBookedAt()).lastRideCompletedAt(source.getLastRideCompletedAt())
                .createdAt(source.getCreatedAt()).updatedAt(source.getUpdatedAt())
                .build();
    }
}
