package com.swiftapp_backend.analyticalservice.analytics.application.service;

import com.swiftapp_backend.analyticalservice.analytics.api.dto.DashboardMetricsDto;
import com.swiftapp_backend.analyticalservice.analytics.api.dto.DriverAnalyticsResponseDto;
import com.swiftapp_backend.analyticalservice.analytics.api.dto.RiderAnalyticsResponseDto;
import com.swiftapp_backend.analyticalservice.analytics.domain.model.DriverAnalytics;
import com.swiftapp_backend.analyticalservice.analytics.domain.model.RiderAnalytics;
import com.swiftapp_backend.analyticalservice.analytics.domain.repository.DriverAnalyticsRepository;
import com.swiftapp_backend.analyticalservice.analytics.domain.repository.RiderAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryService {
    private final DriverAnalyticsRepository driverAnalyticsRepository;
    private final RiderAnalyticsRepository riderAnalyticsRepository;

    public DriverAnalyticsResponseDto getDriverAnalytics(Long driverId) {
        return toDriverResponse(driverAnalyticsRepository.findByDriverId(driverId)
                .orElseGet(() -> DriverAnalytics.builder().driverId(driverId).build()));
    }

    public RiderAnalyticsResponseDto getRiderAnalytics(Long riderId) {
        return toRiderResponse(riderAnalyticsRepository.findByRiderId(riderId)
                .orElseGet(() -> RiderAnalytics.builder().riderId(riderId).build()));
    }

    public DashboardMetricsDto getDashboardMetrics() {
        return DashboardMetricsDto.builder()
                .trackedDrivers(driverAnalyticsRepository.count())
                .trackedRiders(riderAnalyticsRepository.count())
                .totalCompletedTrips(driverAnalyticsRepository.sumCompletedTrips())
                .totalCancelledTrips(driverAnalyticsRepository.sumCancelledTrips())
                .totalEarnings(driverAnalyticsRepository.sumTotalEarnings())
                .totalSpent(riderAnalyticsRepository.sumTotalSpent())
                .build();
    }

    public Double getDriverEarnings(Long driverId, String period) {
        DriverAnalytics analytics = driverAnalyticsRepository.findByDriverId(driverId)
                .orElseGet(() -> DriverAnalytics.builder().driverId(driverId).build());
        if ("today".equalsIgnoreCase(period)) {
            return value(analytics.getTodayEarnings());
        }
        if ("week".equalsIgnoreCase(period) || "weekly".equalsIgnoreCase(period)) {
            return value(analytics.getWeeklyEarnings());
        }
        if ("month".equalsIgnoreCase(period) || "monthly".equalsIgnoreCase(period)) {
            return value(analytics.getMonthlyEarnings());
        }
        return value(analytics.getTotalEarnings());
    }

    private DriverAnalyticsResponseDto toDriverResponse(DriverAnalytics analytics) {
        return DriverAnalyticsResponseDto.builder()
                .driverId(analytics.getDriverId())
                .totalRides((int) value(analytics.getTotalTrips()))
                .completedRides((int) value(analytics.getCompletedTrips()))
                .cancelledRides((int) value(analytics.getCancelledTrips()))
                .totalEarnings(value(analytics.getTotalEarnings()))
                .averageRating(value(analytics.getAverageRating()))
                .acceptanceRate(value(analytics.getAcceptanceRate()))
                .cancellationRate(value(analytics.getCancellationRate()))
                .totalOnlineHours(value(analytics.getOnlineHours()))
                .averageRideDistance(averageDistance(analytics))
                .build();
    }

    private RiderAnalyticsResponseDto toRiderResponse(RiderAnalytics analytics) {
        return RiderAnalyticsResponseDto.builder()
                .riderId(analytics.getRiderId())
                .totalRides((int) value(analytics.getTotalBookings()))
                .completedRides((int) value(analytics.getCompletedTrips()))
                .cancelledRides((int) value(analytics.getCancelledTrips()))
                .totalSpent(value(analytics.getTotalSpent()))
                .averageRating(value(analytics.getAverageDriverRating()))
                .averageFare(value(analytics.getAverageRideCost()))
                .ridesThisMonth(0)
                .build();
    }

    private double averageDistance(DriverAnalytics analytics) {
        long trips = value(analytics.getCompletedTrips());
        return trips == 0 ? 0.0 : value(analytics.getTotalDistanceKm()) / (double) trips;
    }

    private static long value(Long value) {
        return value == null ? 0L : value;
    }

    private static double value(Double value) {
        return value == null ? 0.0 : value;
    }
}
