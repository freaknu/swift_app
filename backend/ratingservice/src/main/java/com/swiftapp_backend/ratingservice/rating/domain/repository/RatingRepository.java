package com.swiftapp_backend.ratingservice.rating.domain.repository;

import com.swiftapp_backend.ratingservice.rating.domain.model.RatingModel;

import java.util.Optional;

public interface RatingRepository {
    Optional<RatingModel> findByDriverId(Long driverId);

    Optional<RatingModel> findByRiderId(Long riderId);

    Optional<RatingModel> findById(Long id);

    RatingModel save(RatingModel rating);
}
