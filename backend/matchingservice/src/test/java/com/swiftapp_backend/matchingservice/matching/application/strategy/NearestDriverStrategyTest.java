package com.swiftapp_backend.matchingservice.matching.application.strategy;

import com.swiftapp_backend.matchingservice.matching.domain.dto.MatchingContext;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStrategyType;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearestDriverStrategyTest {

    private final NearestDriverStrategy strategy = new NearestDriverStrategy();

    @Test
    void ordersByDistanceAscending() {
        List<DriverCandidate> ordered = strategy.order(List.of(
                candidate(101L, 2.5),
                candidate(102L, 0.4),
                candidate(103L, 1.1)), context());

        assertEquals(List.of(102L, 103L, 101L),
                ordered.stream().map(DriverCandidate::getDriverId).toList());
    }

    @Test
    void emptyStaysEmpty() {
        assertTrue(strategy.order(List.of(), context()).isEmpty());
    }

    @Test
    void typeIsNearest() {
        assertEquals(MatchingStrategyType.NEAREST, strategy.type());
    }

    private static MatchingContext context() {
        return MatchingContext.builder().rideId(1L).build();
    }

    private static DriverCandidate candidate(Long driverId, double distanceKm) {
        return DriverCandidate.builder()
                .driverId(driverId)
                .distanceKm(distanceKm)
                .status(DriverStatusEventEnum.ACTIVE)
                .vehicleType(VehicleTypeEnum.SEDAN)
                .build();
    }
}
