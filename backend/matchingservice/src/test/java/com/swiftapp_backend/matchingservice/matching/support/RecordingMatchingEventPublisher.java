package com.swiftapp_backend.matchingservice.matching.support;

import com.swiftapp_backend.matchingservice.matching.domain.events.DriverMatchedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideOfferBroadcastEvent;
import com.swiftapp_backend.matchingservice.matching.infrastructure.messaging.kafka.MatchingEventPublisher;

import java.util.ArrayList;
import java.util.List;

/** Records published events instead of touching Kafka. */
public class RecordingMatchingEventPublisher extends MatchingEventPublisher {

    public record OfferBroadcast(Long rideId, Long riderId, int attempt,
                                 double radiusKm, List<Long> driverIds) {
    }

    public record OfferWithdrawn(Long rideId, List<Long> driverIds, String reason) {
    }

    public record MatchFailed(Long rideId, Long riderId, String reason) {
    }

    public final List<DriverMatchedEvent> matched = new ArrayList<>();

    public final List<OfferBroadcast> broadcasts = new ArrayList<>();

    public final List<OfferWithdrawn> withdrawn = new ArrayList<>();

    public final List<MatchFailed> matchFailed = new ArrayList<>();

    public RecordingMatchingEventPublisher() {
        super(null, null, null);
    }

    @Override
    public void publishDriverMatched(DriverMatchedEvent event) {
        matched.add(event);
    }

    @Override
    public void publishOfferBroadcast(RideOfferBroadcastEvent event) {
        broadcasts.add(new OfferBroadcast(
                event.getRideId(),
                event.getRiderId(),
                event.getAttempt(),
                event.getRadiusKm(),
                event.getTargets().stream()
                        .map(RideOfferBroadcastEvent.OfferTarget::getDriverId)
                        .toList()));
    }

    @Override
    public void publishOfferWithdrawn(Long rideId, List<Long> driverIds, String reason) {
        withdrawn.add(new OfferWithdrawn(rideId, driverIds, reason));
    }

    @Override
    public void publishMatchFailed(Long rideId, Long riderId, String reason) {
        matchFailed.add(new MatchFailed(rideId, riderId, reason));
    }
}
