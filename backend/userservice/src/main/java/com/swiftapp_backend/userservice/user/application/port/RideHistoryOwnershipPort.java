package com.swiftapp_backend.userservice.user.application.port;

import java.util.Optional;

public interface RideHistoryOwnershipPort {
    Optional<Long> riderProfileIdForRide(Long rideId);
}
