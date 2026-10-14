package com.swiftapp_backend.matchingservice.matching.domain.repository;

import com.swiftapp_backend.matchingservice.matching.domain.dto.DriverFindDto;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverLocation;

import java.util.List;

public interface DriverMatchingRepository {

    void updateDriverLocation(DriverLocation location);

    List<DriverLocation> findAllNearbyDriver(DriverFindDto data);

    /**
     * Flips a driver's matchability (e.g. to ALREADY_ACQUIRED once matched)
     * so the GEO filter skips them.
     */
    void updateDriverStatus(Long driverId, DriverStatusEventEnum status);

}
