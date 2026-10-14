package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverStaticsDto;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverDetailMapper;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverStatisticsModel;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverRideStatisticsEventType;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverPreferenceRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRideStatisticsEventRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverStaticsService {
    private final DriverPreferenceRepository driverPreferenceRepository;
    private final DriverStatisticsRepository driverStatisticsRepository;
    private final DriverRepository driverRepository;
    private final DriverRideStatisticsEventRepository rideEvents;

    @Transactional
    public DriverStaticsDto getDriverStatics(Long authUserId) throws Exception {
        DriverModel existingDriver = driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(()->new Exception("User not found with userid  " + authUserId));

        DriverStatisticsModel stats = driverStatisticsRepository.findByDriverId(existingDriver.getId())
                .orElseGet(() -> {
                    DriverStatisticsModel newStats = new DriverStatisticsModel();
                    newStats.setCompletedTrips(0);
                    newStats.setCancelledTrips(0);
                    newStats.setAverageRating(5.0);
                    newStats.setAcceptanceRate(100.0);
                    newStats.setCompletionRate(100.0);
                    newStats.setLifetimeDistance(0.0);
                    newStats.setLifetimeEarning(0.0);
                    newStats.setDriver(existingDriver);
                    newStats.setCreatedAt(LocalDateTime.now());
                    newStats.setUpdatedAt(LocalDateTime.now());
                    return driverStatisticsRepository.save(newStats);
                });

        return DriverStaticsDto
                .builder()
                .completedTrips(stats.getCompletedTrips() != null ? stats.getCompletedTrips() : 0)
                .cancelledTrips(stats.getCancelledTrips() != null ? stats.getCancelledTrips() : 0)
                .averageRating(stats.getAverageRating())
                .acceptanceRate(stats.getAcceptanceRate())
                .completionRate(stats.getCompletionRate())
                .lifetimeDistance(stats.getLifetimeDistance())
                .lifetimeEarning(stats.getLifetimeEarning())
                .driverData(DriverDetailMapper.toResponseDto(existingDriver))
                .build();
    }

    @Transactional
    public void recordRideCompleted(Long rideId, Long driverId, double fare, double distance) {
        if (rideId == null) throw new IllegalArgumentException("Completed ride event requires rideId");
        log.info("Recording ride completed for rideId={}, driverId={}", rideId, driverId);
        if (!rideEvents.lockDriver(driverId) || rideEvents.hasRecordedRide(rideId)) return;
        driverRepository.findById(driverId).ifPresent(driver -> {
            DriverStatisticsModel stats = driverStatisticsRepository.findByDriverId(driver.getId())
                    .orElseGet(() -> {
                        DriverStatisticsModel s = new DriverStatisticsModel();
                        s.setCompletedTrips(0);
                        s.setCancelledTrips(0);
                        s.setAverageRating(5.0);
                        s.setAcceptanceRate(100.0);
                        s.setCompletionRate(100.0);
                        s.setLifetimeDistance(0.0);
                        s.setLifetimeEarning(0.0);
                        s.setDriver(driver);
                        return s;
                    });
            stats.setCompletedTrips(stats.getCompletedTrips() == null ? 1 : stats.getCompletedTrips() + 1);
            stats.setLifetimeEarning(stats.getLifetimeEarning() + fare);
            stats.setLifetimeDistance(stats.getLifetimeDistance() + distance);
            stats.setUpdatedAt(LocalDateTime.now());
            driverStatisticsRepository.save(stats);
            rideEvents.recordRide(rideId, driverId, DriverRideStatisticsEventType.COMPLETED);
        });
    }

    @Transactional
    public void recordRideCancelled(Long rideId, Long driverId) {
        if (rideId == null) throw new IllegalArgumentException("Cancelled ride event requires rideId");
        log.info("Recording ride cancelled for rideId={}, driverId={}", rideId, driverId);
        if (!rideEvents.lockDriver(driverId) || rideEvents.hasRecordedRide(rideId)) return;
        driverRepository.findById(driverId).ifPresent(driver -> {
            DriverStatisticsModel stats = driverStatisticsRepository.findByDriverId(driver.getId())
                    .orElseGet(() -> {
                        DriverStatisticsModel s = new DriverStatisticsModel();
                        s.setCompletedTrips(0);
                        s.setCancelledTrips(0);
                        s.setAverageRating(5.0);
                        s.setDriver(driver);
                        return s;
                    });
            stats.setCancelledTrips(stats.getCancelledTrips() == null ? 1 : stats.getCancelledTrips() + 1);
            stats.setUpdatedAt(LocalDateTime.now());
            driverStatisticsRepository.save(stats);
            rideEvents.recordRide(rideId, driverId, DriverRideStatisticsEventType.CANCELLED);
        });
    }

    @Transactional
    public void recordRatingUpdated(Long driverId, double rating) {
        log.info("Recording rating update for driverId={}, rating={}", driverId, rating);
        driverRepository.findById(driverId).ifPresent(driver -> {
            DriverStatisticsModel stats = driverStatisticsRepository.findByDriverId(driver.getId())
                    .orElseGet(() -> {
                        DriverStatisticsModel s = new DriverStatisticsModel();
                        s.setCompletedTrips(0);
                        s.setCancelledTrips(0);
                        s.setAverageRating(5.0);
                        s.setDriver(driver);
                        return s;
                    });
            stats.setAverageRating(rating);
            stats.setUpdatedAt(LocalDateTime.now());
            driverStatisticsRepository.save(stats);
        });
    }
}
