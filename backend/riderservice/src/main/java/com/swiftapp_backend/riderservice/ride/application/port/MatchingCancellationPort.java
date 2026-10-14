package com.swiftapp_backend.riderservice.ride.application.port;

/** Synchronous fence against an in-flight driver offer during rider cancellation. */
public interface MatchingCancellationPort {
    enum Result { CANCELLED, ALREADY_ACCEPTED }

    Result cancelPendingRide(Long rideId);
}
