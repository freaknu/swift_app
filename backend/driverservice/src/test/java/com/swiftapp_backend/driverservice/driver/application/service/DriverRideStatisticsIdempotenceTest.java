package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverStatisticsRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRideStatisticsEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DriverRideStatisticsIdempotenceTest {
    @Autowired private DriverStaticsService statistics;
    @Autowired private DriverRepository drivers;
    @Autowired private DriverStatisticsRepository storedStatistics;
    @Autowired private SpringDataDriverRideStatisticsEventRepository events;

    @Test
    void terminalRideEventsCountOnceAcrossCommittedDeliveries() {
        DriverModel driver = drivers.save(DriverModel.builder()
                .authUserId(98_001L).driverName("Statistics driver")
                .email("stats-driver@example.invalid").phoneNumber("9800100000")
                .build());
        Long driverId = driver.getId();

        statistics.recordRideCompleted(99_001L, driverId, 120.0, 4.5);
        statistics.recordRideCompleted(99_001L, driverId, 900.0, 90.0);
        statistics.recordRideCancelled(99_001L, driverId);
        statistics.recordRideCancelled(99_002L, driverId);
        statistics.recordRideCancelled(99_002L, driverId);

        var saved = storedStatistics.findByDriverId(driverId).orElseThrow();
        assertThat(saved.getCompletedTrips()).isEqualTo(1);
        assertThat(saved.getCancelledTrips()).isEqualTo(1);
        assertThat(saved.getLifetimeEarning()).isEqualTo(120.0);
        assertThat(saved.getLifetimeDistance()).isEqualTo(4.5);
        assertThat(events.existsByRideId(99_001L)).isTrue();
        assertThat(events.existsByRideId(99_002L)).isTrue();
    }
}
