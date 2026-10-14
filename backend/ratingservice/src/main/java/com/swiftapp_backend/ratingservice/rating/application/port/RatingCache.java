package com.swiftapp_backend.ratingservice.rating.application.port;

import com.swiftapp_backend.ratingservice.rating.domain.model.RateUserModel;

import java.util.List;
import java.util.Optional;

public interface RatingCache {
    Optional<List<RateUserModel>> findRatings(Long ratingId);

    void storeRatings(Long ratingId, List<RateUserModel> ratings);
}
