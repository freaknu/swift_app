package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverStatisticsModel;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverStatisticsEntity;

public final class DriverStatisticsPersistenceMapper {
    private DriverStatisticsPersistenceMapper() {
    }

    public static DriverStatisticsModel toDomain(DriverStatisticsEntity source) {
        if (source == null) {
            return null;
        }

        DriverStatisticsModel target = new DriverStatisticsModel();
        target.setId(source.getId());
        target.setCompletedTrips(source.getCompletedTrips());
        target.setCancelledTrips(source.getCancelledTrips());
        target.setAverageRating(source.getAverageRating());
        target.setAcceptanceRate(source.getAcceptanceRate());
        target.setCompletionRate(source.getCompletionRate());
        target.setLifetimeDistance(source.getLifetimeDistance());
        target.setLifetimeEarning(source.getLifetimeEarning());
        target.setDriver(DriverPersistenceMapper.toDomainReference(source.getDriver()));
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    public static DriverStatisticsEntity toEntity(
            DriverStatisticsModel source,
            DriverEntity driver
    ) {
        if (source == null) {
            return null;
        }

        DriverStatisticsEntity target = new DriverStatisticsEntity();
        target.setId(source.getId());
        updateEntity(source, target, driver);
        return target;
    }

    public static void updateEntity(
            DriverStatisticsModel source,
            DriverStatisticsEntity target,
            DriverEntity driver
    ) {
        target.setCompletedTrips(source.getCompletedTrips());
        target.setCancelledTrips(source.getCancelledTrips());
        target.setAverageRating(source.getAverageRating());
        target.setAcceptanceRate(source.getAcceptanceRate());
        target.setCompletionRate(source.getCompletionRate());
        target.setLifetimeDistance(source.getLifetimeDistance());
        target.setLifetimeEarning(source.getLifetimeEarning());
        target.setDriver(driver);
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }
}
