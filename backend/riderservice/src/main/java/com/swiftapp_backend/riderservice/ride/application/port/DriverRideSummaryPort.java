package com.swiftapp_backend.riderservice.ride.application.port;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.AssignedDriverSummaryDto;

public interface DriverRideSummaryPort {
    AssignedDriverSummaryDto get(Long driverId, Long vehicleId);
}
