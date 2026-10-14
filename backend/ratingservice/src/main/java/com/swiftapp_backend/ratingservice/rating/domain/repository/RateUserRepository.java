package com.swiftapp_backend.ratingservice.rating.domain.repository;

import com.swiftapp_backend.ratingservice.rating.domain.model.RateUserModel;

import java.util.List;
import java.util.Optional;

public interface RateUserRepository {
    Optional<RateUserModel> findById(Long id);

    Optional<RateUserModel> findByRideIdAndRaterAuthUserId(Long rideId, Long authUserId);

    List<RateUserModel> findAllByRating_Id(Long ratingId);

    RateUserModel save(RateUserModel rating);
}
