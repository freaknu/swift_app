package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.AssignedRiderSummaryDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.application.port.RideIdentityPort;
import com.swiftapp_backend.riderservice.ride.application.service.RideIdentityService;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RideRiderSummaryControllerTest {
    @Test
    void onlyAssignedDriverCanReadRiderName() {
        AtomicInteger lookups = new AtomicInteger();
        RideService rides = new RideService(null, null, null, null, null, null, null, null, null, null) {
            @Override public RideDto getRide(Long rideId) {
                return RideDto.builder().id(rideId).riderId(91L).driverId(55L).build();
            }
        };
        RideIdentityPort identityPort = new RideIdentityPort() {
            @Override public Long riderAuthUserId(Long id) { return null; }
            @Override public Long driverAuthUserId(Long id) { return null; }
            @Override public Long riderIdForAuthUser(Long id) { return null; }
            @Override public Long driverIdForAuthUser(Long id) { return id == 42L ? 55L : null; }
        };
        RideRiderSummaryController controller = new RideRiderSummaryController(rides,
                new RideIdentityService(identityPort), riderId -> {
                    lookups.incrementAndGet();
                    return new AssignedRiderSummaryDto(riderId, "Riya");
                });
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/api/v1/ride/rides/7/rider-summary");

        assertEquals("Riya", controller.get(42L, 7L, request).getData().riderName());
        assertEquals(403, assertThrows(ApiException.class,
                () -> controller.get(99L, 7L, request)).getStatus().value());
        assertEquals(1, lookups.get());
    }
}
