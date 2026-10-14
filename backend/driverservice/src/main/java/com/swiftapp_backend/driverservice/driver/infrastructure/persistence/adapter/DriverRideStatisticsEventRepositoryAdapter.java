package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.domain.enums.DriverRideStatisticsEventType;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRideStatisticsEventRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverRideStatisticsEventEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRideStatisticsEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DriverRideStatisticsEventRepositoryAdapter implements DriverRideStatisticsEventRepository {
    private final EntityManager entityManager;
    private final SpringDataDriverRideStatisticsEventRepository events;

    @Override
    public boolean lockDriver(Long driverId) {
        return entityManager.find(DriverEntity.class, driverId, LockModeType.PESSIMISTIC_WRITE) != null;
    }

    @Override
    public boolean hasRecordedRide(Long rideId) {
        return events.existsByRideId(rideId);
    }

    @Override
    public void recordRide(Long rideId, Long driverId, DriverRideStatisticsEventType eventType) {
        DriverRideStatisticsEventEntity event = new DriverRideStatisticsEventEntity();
        event.setRideId(rideId);
        event.setDriverId(driverId);
        event.setEventType(eventType);
        events.save(event);
    }
}
