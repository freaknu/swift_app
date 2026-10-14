package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.AssignedDriverSummaryDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.application.port.DriverRideSummaryPort;
import com.swiftapp_backend.riderservice.ride.application.port.RideIdentityPort;
import com.swiftapp_backend.riderservice.ride.application.service.RideIdentityService;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RideDriverSummaryControllerTest {
    private final StubRides rides = new StubRides();
    private final StubSummaries summaries = new StubSummaries();
    private final RideDriverSummaryController controller = new RideDriverSummaryController(
            rides, new RideIdentityService(new StubIdentities()), summaries);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET",
            "/api/v1/ride/rides/7/driver-summary");

    @Test
    void onlyRiderOwnerReceivesAssignedSummary() {
        rides.ride = RideDto.builder().id(7L).riderId(91L)
                .driverId(55L).vehicleId(77L).build();

        assertEquals("Alex", controller.get(901L, 7L, request).getData().driverName());
        assertEquals(1, summaries.calls);
        assertEquals(403, assertThrows(ApiException.class,
                () -> controller.get(999L, 7L, request)).getStatus().value());
        assertEquals(1, summaries.calls);
    }

    @Test
    void unassignedRideHasNoDriverSummary() {
        rides.ride = RideDto.builder().id(7L).riderId(91L).build();

        assertNull(controller.get(901L, 7L, request).getData());
        assertEquals(0, summaries.calls);
    }

    private static final class StubRides extends RideService {
        private RideDto ride;
        private StubRides() { super(null, null, null, null, null, null, null, null, null, null); }
        @Override public RideDto getRide(Long rideId) { return ride; }
    }

    private static final class StubSummaries implements DriverRideSummaryPort {
        private int calls;
        @Override public AssignedDriverSummaryDto get(Long driverId, Long vehicleId) {
            calls++;
            return new AssignedDriverSummaryDto(driverId, "Alex", null, vehicleId,
                    "SEDAN", "Honda", "City", "Black", "KA01AB1234", 4, true);
        }
    }

    private static final class StubIdentities implements RideIdentityPort {
        @Override public Long riderAuthUserId(Long profileId) { return null; }
        @Override public Long driverAuthUserId(Long profileId) { return null; }
        @Override public Long riderIdForAuthUser(Long authUserId) {
            return authUserId == 901L ? 91L : null;
        }
        @Override public Long driverIdForAuthUser(Long authUserId) { return null; }
    }
}
