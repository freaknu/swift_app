package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideStatusHistoryDto;
import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareDto;
import com.swiftapp_backend.riderservice.ride.application.port.RideIdentityPort;
import com.swiftapp_backend.riderservice.ride.application.service.RideIdentityService;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.ride.application.service.FareService;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RideReadAccessTest {
    private final StubRideService rides = new StubRideService();
    private final RideIdentityPort identities = new RideIdentityPort() {
        @Override public Long riderAuthUserId(Long profileId) { return Long.valueOf(101).equals(profileId) ? 7L : null; }
        @Override public Long driverAuthUserId(Long profileId) { return Long.valueOf(201).equals(profileId) ? 8L : null; }
        @Override public Long riderIdForAuthUser(Long authUserId) { return null; }
        @Override public Long driverIdForAuthUser(Long authUserId) { return null; }
    };
    private final RideController controller = new RideController(rides, new RideIdentityService(identities));

    @Test
    void detailAndHistoryRequireAnActualRideParticipant() {
        rides.ride = RideDto.builder().id(501L).riderId(101L).driverId(201L).build();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ride/getRideDetail/501");

        assertEquals(501L, controller.getRide(7L, 501L, request).getBody().getData().getId());
        assertEquals(501L, controller.getRide(8L, 501L, request).getBody().getData().getId());
        assertEquals(200, controller.getRideHistory(7L, 501L, request).getStatusCode().value());
        assertEquals(403, assertThrows(ApiException.class,
                () -> controller.getRide(9L, 501L, request)).getStatus().value());
        assertEquals(403, assertThrows(ApiException.class,
                () -> controller.getRideHistory(9L, 501L, request)).getStatus().value());
    }

    @Test
    void internalRideLookupRejectsNonLoopbackBeforeReadingRide() {
        InternalRideController internal = new InternalRideController(rides);
        MockHttpServletRequest remote = new MockHttpServletRequest("GET", "/internal/rides/501");
        remote.setRemoteAddr("203.0.113.1");

        assertEquals(403, assertThrows(ApiException.class,
                () -> internal.getRide(501L, remote)).getStatus().value());
        assertEquals(0, rides.readCalls);
    }

    @Test
    void internalDriverActiveRideLookupRejectsRemoteCaller() {
        InternalDriverRideController internal = new InternalDriverRideController(rides);
        MockHttpServletRequest remote = new MockHttpServletRequest("GET",
                "/internal/rides/drivers/201/active-id");
        remote.setRemoteAddr("203.0.113.1");

        assertEquals(403, assertThrows(ApiException.class,
                () -> internal.activeDriverRideId(201L, remote)).getStatus().value());
    }

    @Test
    void persistedFareIsOnlyReturnedToRideParticipants() {
        rides.ride = RideDto.builder().id(502L).riderId(101L).driverId(201L).build();
        StubFareService fares = new StubFareService();
        FareController fareController = new FareController(fares, rides,
                new RideIdentityService(identities));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/fare/rides/502/fare");

        assertEquals(403, assertThrows(ApiException.class,
                () -> fareController.getRideFare(9L, 502L, request)).getStatus().value());
        assertEquals(0, fares.readCalls);
        assertEquals(200, fareController.getRideFare(7L, 502L, request).getStatusCode().value());
        assertEquals(200, fareController.getRideFare(8L, 502L, request).getStatusCode().value());
        assertEquals(2, fares.readCalls);
    }

    private static final class StubRideService extends RideService {
        private RideDto ride;
        private int readCalls;

        private StubRideService() {
            super(null, null, null, null, null, null, null, null, null, null);
        }

        @Override public RideDto getRide(Long rideId) {
            readCalls++;
            return ride;
        }

        @Override public List<RideStatusHistoryDto> getRideHistory(Long rideId) {
            return List.of();
        }
    }

    private static final class StubFareService extends FareService {
        private int readCalls;

        private StubFareService() {
            super(null, null, null, null, null);
        }

        @Override public FareDto getFareByRideId(Long rideId) {
            readCalls++;
            return FareDto.builder().build();
        }
    }
}
