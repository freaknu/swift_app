package com.swiftapp_backend.driverservice.driver.domain.repository;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverEarningSummaryModel;

import java.util.Optional;

public interface DriverEarningRepository {
    Optional<DriverEarningSummaryModel> findByDriverId(Long driverId);
    DriverEarningSummaryModel save(DriverEarningSummaryModel model);
}
