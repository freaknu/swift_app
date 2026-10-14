package com.swiftapp_backend.matchingservice.matching.support;

import com.swiftapp_backend.matchingservice.matching.domain.dto.DriverFindDto;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverLocation;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverMatchingRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Records status updates; location search is unused here. */
public class StubDriverMatchingRepository implements DriverMatchingRepository {

    public final Map<Long, DriverStatusEventEnum> statusUpdates = new HashMap<>();

    @Override
    public void updateDriverLocation(DriverLocation location) {
    }

    @Override
    public List<DriverLocation> findAllNearbyDriver(DriverFindDto data) {
        return List.of();
    }

    @Override
    public void updateDriverStatus(Long driverId, DriverStatusEventEnum status) {
        statusUpdates.put(driverId, status);
    }
}
