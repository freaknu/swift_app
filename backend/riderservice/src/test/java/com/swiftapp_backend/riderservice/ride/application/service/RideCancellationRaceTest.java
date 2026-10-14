package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.CancelRideRequest;
import com.swiftapp_backend.riderservice.ride.application.port.MatchingCancellationPort;
import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideRepository;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideCancellationRaceTest {

    @Test
    void pendingRideCannotBeCancelledAfterMatchingAcceptedDriver() {
        RideModel ride = RideModel.builder().id(11L).riderId(7L)
                .currentStatus(RideStatus.MATCHING).build();
        RideRepository repository = (RideRepository) Proxy.newProxyInstance(
                RideRepository.class.getClassLoader(), new Class<?>[] {RideRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByIdForUpdate")) return Optional.of(ride);
                    throw new AssertionError("Cancelled ride mutated through " + method.getName());
                });
        MatchingCancellationPort matching = rideId ->
                MatchingCancellationPort.Result.ALREADY_ACCEPTED;
        RideService service = new RideService(repository, null, null, null, null,
                null, null, null, null, matching);

        assertThatThrownBy(() -> service.cancelRide(11L,
                new CancelRideRequest(7L, ChangedByType.RIDER, "Changed plans")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("driver accepted");
        assertThat(ride.getCurrentStatus()).isEqualTo(RideStatus.MATCHING);
        assertThat(ride.getCancelledAt()).isNull();
    }
}
