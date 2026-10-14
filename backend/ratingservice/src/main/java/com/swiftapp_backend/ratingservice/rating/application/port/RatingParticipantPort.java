package com.swiftapp_backend.ratingservice.rating.application.port;

import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;

public interface RatingParticipantPort {
    record Ride(Long id, Long riderId, Long driverId, String currentStatus) {}

    Ride ride(Long rideId);

    Long profileIdForAuthUser(Long authUserId, UserType role);

    Long authUserIdForProfile(Long profileId, UserType role);
}
