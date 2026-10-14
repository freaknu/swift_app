package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideActionRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.CancelRideRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideRequestDto;
import com.swiftapp_backend.riderservice.ride.application.port.RideIdentityPort;
import com.swiftapp_backend.riderservice.ride.application.service.RideIdentityService;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideType;
import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideControllerIdentityTest {
    private Long riderProfileId = 101L;
    private Long driverProfileId = 201L;
    private final RideIdentityPort identityPort = new RideIdentityPort() {
        @Override public Long riderAuthUserId(Long profileId) { return 7L; }
        @Override public Long driverAuthUserId(Long profileId) { return 8L; }
        @Override
        public Long riderIdForAuthUser(Long authUserId) {
            return riderProfileId;
        }

        @Override
        public Long driverIdForAuthUser(Long authUserId) {
            return driverProfileId;
        }
    };
    // These checks must fail before any ride operation is reached.
    private final RideService rideService = new RideService(
            null, null, null, null, null, null, null, null, null, null);
    private final RideController controller = new RideController(
            rideService, new RideIdentityService(identityPort));

    @Test
    void requestRejectsBodyIdentityDifferentFromAuthenticatedUser() {
        RideRequestDto body = new RideRequestDto(9L, null, 101L,
                "STANDARD", null);

        assertThatThrownBy(() -> controller.requestRide(7L, body, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("authUserId");
    }

    @Test
    void requestRejectsAnotherRiderProfileEvenWhenAuthIdMatches() {
        RideRequestDto body = new RideRequestDto(7L, null, 999L,
                "STANDARD", null);

        assertThatThrownBy(() -> controller.requestRide(7L, body, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not own");
    }

    @Test
    void driverActionRejectsDifferentDriverProfileBeforeRideMutation() {
        assertThatThrownBy(() -> controller.driverArrived(7L, 501L,
                new RideActionRequest(202L), null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not own");
    }

    @Test
    void legacyDirectAcceptCannotAssignARequestedRide() {
        assertThatThrownBy(() -> controller.acceptRide(7L, 501L,
                null, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("targeted driver offer");
    }

    @Test
    void publicCancellationRejectsSystemAndAdminBeforeRideMutation() {
        for (ChangedByType type : new ChangedByType[]{ChangedByType.SYSTEM, ChangedByType.ADMIN}) {
            assertThatThrownBy(() -> controller.cancelRide(7L, 501L,
                    new CancelRideRequest(7L, type, "Changed plans"), null))
                    .isInstanceOf(ApiException.class)
                    .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ApiException) error).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN))
                    .hasMessageContaining("rider or driver");
        }
    }
}
