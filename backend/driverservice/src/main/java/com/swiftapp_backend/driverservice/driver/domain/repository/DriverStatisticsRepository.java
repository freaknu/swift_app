package com.swiftapp_backend.driverservice.driver.domain.repository;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverStatisticsModel;

import java.util.Optional;

public interface DriverStatisticsRepository {

    Optional<DriverStatisticsModel> findById(Long id);

    Optional<DriverStatisticsModel> findByDriverId(Long driverId);

    DriverStatisticsModel save(DriverStatisticsModel statistics);
}
