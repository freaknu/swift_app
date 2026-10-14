package com.swiftapp_backend.riderservice.ride.application.port;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.AssignedRiderSummaryDto;

public interface RiderRideSummaryPort {
    AssignedRiderSummaryDto get(Long riderId);
}
