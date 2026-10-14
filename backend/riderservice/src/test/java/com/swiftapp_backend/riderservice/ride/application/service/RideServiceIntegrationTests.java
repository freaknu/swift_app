package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.AcceptRideRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.CancelRideRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideRequestDto;
import com.swiftapp_backend.riderservice.ride.application.port.MatchingCancellationPort;
import com.swiftapp_backend.riderservice.ride.application.port.MessagePublisherPort;
import com.swiftapp_backend.riderservice.ride.domain.event.RideRequestedEventDto;
import com.swiftapp_backend.riderservice.ride.domain.event.RideCompletedEventDto;
import com.swiftapp_backend.riderservice.ride.domain.enums.PaymentStatus;
import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RideServiceIntegrationTests {

    @TestConfiguration
    static class MatchingCancellationStub {
        @Bean
        @Primary
        MatchingCancellationPort matchingCancellationPort() {
            return rideId -> MatchingCancellationPort.Result.CANCELLED;
        }

        @Bean
        @Primary
        RecordingPublisher recordingPublisher() {
            return new RecordingPublisher();
        }
    }

    static class RecordingPublisher implements MessagePublisherPort {
        private final ConcurrentMap<Long, RideRequestedEventDto> requested = new ConcurrentHashMap<>();
        private final ConcurrentMap<Long, RideCompletedEventDto> completed = new ConcurrentHashMap<>();

        @Override
        public void publish(String topic, String key, Object event) {
            if ("ride.requested".equals(topic) && event instanceof RideRequestedEventDto requestedEvent) {
                requested.put(requestedEvent.getRideId(), requestedEvent);
            }
            if ("ride.completed".equals(topic) && event instanceof RideCompletedEventDto completedEvent) {
                completed.put(completedEvent.getRideId(), completedEvent);
            }
        }
    }

    @Autowired
    private RideService rideService;

    @Autowired
    private FareService fareService;

    @Autowired
    private RecordingPublisher publisher;

    private final FareEstimateRequest route = new FareEstimateRequest(
            null, 12.9716, 77.5946, 12.9352, 77.6245, null);

    @Test
    void estimatesFareWithCompleteBreakdown() {
        var estimate = fareService.estimate(route);

        assertThat(estimate.estimatedDistanceKm()).isPositive();
        assertThat(estimate.estimatedDurationSeconds()).isPositive();
        assertThat(estimate.fare().getTotalFare()).isGreaterThanOrEqualTo(new BigDecimal("80.00"));
        assertThat(estimate.fare().getBaseFare()).isNotNull();
        assertThat(estimate.fare().getDistanceFare()).isNotNull();
        assertThat(estimate.fare().getTimeFare()).isNotNull();
        assertThat(estimate.fare().getTax()).isNotNull();
    }

    @Test
    void selectedCanonicalVehicleTypePersistsAndReachesMatchingEvent() throws InterruptedException {
        publisher.requested.clear();
        FareEstimateRequest hatchbackRoute = new FareEstimateRequest(
                null, 12.9716, 77.5946, 12.9352, 77.6245, "HATCHBACK");
        var ride = rideService.requestRide(new RideRequestDto(
                200L, null, 200L, "HATCHBACK", hatchbackRoute));

        assertThat(ride.getRideTypeCode()).isEqualTo("HATCHBACK");
        assertThat(rideService.getRide(ride.getId()).getRideTypeCode()).isEqualTo("HATCHBACK");
        RideRequestedEventDto event = null;
        for (int attempt = 0; attempt < 100 && event == null; attempt++) {
            event = publisher.requested.get(ride.getId());
            if (event == null) Thread.sleep(20);
        }
        assertThat(event).isNotNull();
        assertThat(event.getVehicleType()).isEqualTo("HATCHBACK");
    }

    @Test
    void rejectsMismatchBetweenSelectedTypeAndQuotedType() {
        FareEstimateRequest bikeRoute = new FareEstimateRequest(
                null, 12.9716, 77.5946, 12.9352, 77.6245, "BIKE");
        assertThatThrownBy(() -> rideService.requestRide(new RideRequestDto(
                201L, null, 201L, "HATCHBACK", bikeRoute)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must match");
    }

    @Test
    void executesTheValidRideLifecycleAndRejectsInvalidTransitions() {
        var requested = rideService.requestRide(
                new RideRequestDto(101L, null, 101L, "STANDARD", route));

        assertThat(requested.getRiderId()).isEqualTo(101L);
        assertThat(requested.getCurrentStatus()).isEqualTo(RideStatus.MATCHING);
        assertThat(requested.getFareDetail().getTotalFare()).isPositive();
        assertThat(requested.getRideHistory()).extracting("rideStatus")
                .containsExactly(RideStatus.REQUESTED, RideStatus.MATCHING);

        assertThatThrownBy(() -> rideService.requestRide(
                new RideRequestDto(101L, null, 101L, "STANDARD", route)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("active ride");

        var accepted = rideService.acceptRide(requested.getId(), new AcceptRideRequest(201L, 301L));
        assertThat(accepted.getCurrentStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
        assertThat(accepted.getDriverId()).isEqualTo(201L);
        assertThat(accepted.getVehicleId()).isEqualTo(301L);

        assertThatThrownBy(() -> rideService.startRide(requested.getId(), 201L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not allowed");

        assertThat(rideService.driverArrived(requested.getId(), 201L).getCurrentStatus())
                .isEqualTo(RideStatus.DRIVER_ARRIVED);
        assertThat(rideService.startRide(requested.getId(), 201L).getCurrentStatus())
                .isEqualTo(RideStatus.STARTED);

        var completed = rideService.completeRide(requested.getId(), 201L);
        assertThat(completed.getCurrentStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(completed.getIsCompleted()).isTrue();
        assertThat(completed.getCompletedAt()).isNotNull();
        RideCompletedEventDto event = publisher.completed.get(completed.getId());
        assertThat(event).isNotNull();
        assertThat(event.getTotalFare()).isEqualByComparingTo(completed.getFareDetail().getTotalFare());
        assertThat(event.getRiderId()).isEqualTo(101L);
        assertThat(event.getDriverId()).isEqualTo(201L);
        assertThat(event.getCurrency()).isEqualTo(completed.getFareDetail().getCurrency());

        assertThatThrownBy(() -> rideService.cancelRide(requested.getId(),
                new CancelRideRequest(101L, ChangedByType.RIDER, "Changed plans")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    @Test
    void preventsAnotherRiderFromCancellingTheRide() {
        var ride = rideService.requestRide(
                new RideRequestDto(102L, null, 102L, "STANDARD", route));

        assertThatThrownBy(() -> rideService.cancelRide(ride.getId(),
                new CancelRideRequest(999L, ChangedByType.RIDER, "Not my ride")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not own");

        var cancelled = rideService.cancelRide(ride.getId(),
                new CancelRideRequest(102L, ChangedByType.RIDER, "Changed plans"));
        assertThat(cancelled.getCurrentStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(cancelled.getIsRideCancelled()).isTrue();
        assertThat(cancelled.getCancellationReason()).isEqualTo("Changed plans");
    }

    @Test
    void matchedDriverAssignmentIsPersistedAndIdempotent() {
        var requested = rideService.requestRide(
                new RideRequestDto(103L, null, 103L, "STANDARD", route));

        var assigned = rideService.assignMatchedDriver(requested.getId(), 203L, 303L);
        assertThat(assigned.getCurrentStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
        assertThat(assigned.getDriverId()).isEqualTo(203L);
        assertThat(assigned.getVehicleId()).isEqualTo(303L);
        assertThat(rideService.getActiveDriverRideId(203L)).isEqualTo(requested.getId());

        var redelivered = rideService.assignMatchedDriver(requested.getId(), 203L, 303L);
        assertThat(redelivered.getRideHistory()).extracting("rideStatus")
                .containsExactly(RideStatus.REQUESTED, RideStatus.MATCHING,
                        RideStatus.DRIVER_ASSIGNED);
        assertThat(rideService.driverArrived(requested.getId(), 203L).getCurrentStatus())
                .isEqualTo(RideStatus.DRIVER_ARRIVED);
    }

    @Test
    void driverHistoryIsPagedNewestFirstAndExcludesOtherDrivers() {
        var first = rideService.requestRide(
                new RideRequestDto(501L, null, 501L, "STANDARD", route));
        rideService.assignMatchedDriver(first.getId(), 601L, 701L);
        rideService.driverArrived(first.getId(), 601L);
        rideService.startRide(first.getId(), 601L);
        rideService.completeRide(first.getId(), 601L);

        var second = rideService.requestRide(
                new RideRequestDto(502L, null, 502L, "STANDARD", route));
        rideService.assignMatchedDriver(second.getId(), 601L, 701L);

        var newest = rideService.getDriverRides(601L, 0, 1);
        var older = rideService.getDriverRides(601L, 1, 1);
        assertThat(newest.content()).extracting("id").containsExactly(second.getId());
        assertThat(older.content()).extracting("id").containsExactly(first.getId());
        assertThat(older.content().getFirst().getCurrentStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(newest.totalElements()).isEqualTo(2);
        var completed = rideService.getDriverRides(601L, 0, 20, RideStatus.COMPLETED);
        assertThat(completed.content()).extracting("id").containsExactly(first.getId());
        assertThat(completed.totalElements()).isEqualTo(1);
        assertThat(rideService.getDriverRides(601L, 0, 20, RideStatus.CANCELLED).content()).isEmpty();
        assertThat(rideService.getDriverRides(999L, 0, 20).content()).isEmpty();
    }

    @Test
    void failedMatchingEndsRideAllowsRetryAndCannotUndoAssignment() {
        var first = rideService.requestRide(
                new RideRequestDto(104L, null, 104L, "STANDARD", route));

        var staleFailure = rideService.failMatching(first.getId(), 999L, "NO_AVAILABLE_DRIVER");
        assertThat(staleFailure.getCurrentStatus()).isEqualTo(RideStatus.MATCHING);
        assertThat(staleFailure.getRiderId()).isEqualTo(104L);
        assertThat(staleFailure.getRideHistory()).extracting("rideStatus")
                .containsExactly(RideStatus.REQUESTED, RideStatus.MATCHING);
        assertThat(rideService.failMatching(first.getId(), null, "NO_AVAILABLE_DRIVER")
                .getCurrentStatus()).isEqualTo(RideStatus.MATCHING);

        var failed = rideService.failMatching(first.getId(), 104L, "NO_AVAILABLE_DRIVER");
        assertThat(failed.getCurrentStatus()).isEqualTo(RideStatus.MATCH_FAILED);
        assertThat(failed.getRideHistory()).extracting("rideStatus")
                .containsExactly(RideStatus.REQUESTED, RideStatus.MATCHING, RideStatus.MATCH_FAILED);
        assertThat(rideService.failMatching(first.getId(), 104L, "NO_AVAILABLE_DRIVER")
                .getRideHistory()).hasSize(3);

        var retry = rideService.requestRide(
                new RideRequestDto(104L, null, 104L, "STANDARD", route));
        assertThat(retry.getId()).isNotEqualTo(first.getId());
        assertThat(retry.getCurrentStatus()).isEqualTo(RideStatus.MATCHING);

        rideService.assignMatchedDriver(retry.getId(), 204L, 304L);
        assertThat(rideService.failMatching(retry.getId(), 104L, "NO_AVAILABLE_DRIVER")
                .getCurrentStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
    }

    @Test
    void capturedPaymentUpdatesFareIdempotentlyAndRejectsWrongFareOrParticipants() {
        var requested = rideService.requestRide(
                new RideRequestDto(801L, null, 801L, "STANDARD", route));
        rideService.assignMatchedDriver(requested.getId(), 802L, 803L);
        rideService.driverArrived(requested.getId(), 802L);
        rideService.startRide(requested.getId(), 802L);
        var completed = rideService.completeRide(requested.getId(), 802L);
        long amount = completed.getFareDetail().getTotalFare().movePointRight(2).longValueExact();
        String currency = completed.getFareDetail().getCurrency().name();
        assertThatThrownBy(() -> rideService.recordPaymentCompleted(completed.getId(),
                999L, 802L, amount, currency, "pay_verified"))
                .isInstanceOf(ApiException.class).hasMessageContaining("does not match");
        assertThatThrownBy(() -> rideService.recordPaymentCompleted(completed.getId(),
                801L, 802L, amount + 1, currency, "pay_verified"))
                .isInstanceOf(ApiException.class);
        assertThat(rideService.getRide(completed.getId()).getFareDetail().getPaymentStatus())
                .isEqualTo(PaymentStatus.PENDING);
        rideService.recordPaymentCompleted(completed.getId(), 801L, 802L, amount, currency, "pay_verified");
        rideService.recordPaymentCompleted(completed.getId(), 801L, 802L, amount, currency, "pay_verified");
        assertThat(rideService.getRide(completed.getId()).getFareDetail().getPaymentStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
    }
}
