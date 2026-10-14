package com.swiftapp_backend.matchingservice.matching.application.port;

import com.swiftapp_backend.matchingservice.matching.application.service.OfferResolution;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverOfferAcceptedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverOfferRejectedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideRequestedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;

import java.util.Optional;


public interface MatchingModuleApi {
    // save location data of driver
    void saveLocation(Object data);
    void updateDriverStatus(Long driverId, DriverStatusEventEnum status);
    void requestRide(Object data);

    // new matching engine entry points
    void handleRideRequested(RideRequestedEvent event);

    void handleDriverOfferAccepted(DriverOfferAcceptedEvent event);

    void handleDriverOfferRejected(DriverOfferRejectedEvent event);

    OfferResolution acceptOffer(Long rideId, Long driverId);

    OfferResolution acceptOffer(Long rideId, Long driverId, Long vehicleId);

    OfferResolution rejectOffer(Long rideId, Long driverId, String reason);

    DriverOfferRepository.CancelOutcome cancelPendingRide(Long rideId);

    void recordRideCancelled(Long rideId);

    Optional<MatchingState> matchingState(Long rideId);
}
