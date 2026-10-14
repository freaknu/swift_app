package com.swiftapp_backend.matchingservice.matching.application.port;

import com.swiftapp_backend.matchingservice.matching.domain.events.DriverMatchedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideOfferBroadcastEvent;

import java.util.List;

public interface MatchingEventPublisherPort {
    void publishDriverMatched(DriverMatchedEvent event);

    void publishOfferBroadcast(RideOfferBroadcastEvent event);

    void publishOfferWithdrawn(Long rideId, List<Long> driverIds, String reason);

    void publishMatchFailed(Long rideId, Long riderId, String reason);
}
