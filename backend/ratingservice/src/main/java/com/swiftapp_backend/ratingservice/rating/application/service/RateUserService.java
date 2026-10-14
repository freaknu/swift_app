package com.swiftapp_backend.ratingservice.rating.application.service;

import com.swiftapp_backend.ratingservice.rating.domain.model.RateUserModel;
import com.swiftapp_backend.ratingservice.rating.domain.model.RatingModel;
import com.swiftapp_backend.ratingservice.rating.domain.repository.RateUserRepository;
import com.swiftapp_backend.ratingservice.rating.domain.repository.RatingRepository;
import com.swiftapp_backend.ratingservice.rating.application.port.RatingEventPublisher;
import com.swiftapp_backend.ratingservice.shared.ApiException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RateUserService {
    private final RateUserRepository rateUserRepository;
    private final RatingRepository ratingRepository;
    private final RatingEventPublisher ratingEventPublisher;

    public List<RateUserModel> getAllRateUsers(Long rateId) {
        return rateUserRepository.findAllByRating_Id(rateId)
                .stream()
                .filter(rate -> Boolean.TRUE.equals(rate.getIsActive()))
                .toList();
    }

    @Transactional
    public void deleteRating(Long authUserId, Long userRatingId) {
        RateUserModel rating = rateUserRepository.findById(userRatingId)
                .orElseThrow(() -> ApiException.notFound("Rating was not found"));

        if (authUserId == null || !authUserId.equals(rating.getRaterAuthUserId())) {
            throw ApiException.forbidden("Only the rating author may delete it");
        }
        if (!Boolean.TRUE.equals(rating.getIsActive())) return;

        rating.setIsActive(false);
        rateUserRepository.save(rating);
        RatingModel parent = rating.getRating();
        if (parent == null || parent.getAllRatings() == null) {
            throw new IllegalStateException("Rating aggregate is unavailable");
        }
        parent.getAllRatings().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), userRatingId))
                .forEach(candidate -> candidate.setIsActive(false));
        double avg = parent.getAllRatings()
                .stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.getIsActive()))
                .mapToDouble(RateUserModel::getRate)
                .average()
                .orElse(0);
        parent.setAverageRating(avg);
        ratingRepository.save(parent);
        Map<String, Object> deletedEvent = new HashMap<>();
        deletedEvent.put("userRatingId", userRatingId);
        deletedEvent.put("ratingId", parent.getId());
        deletedEvent.put("averageRating", avg);
        deletedEvent.put("rating", avg);
        if (parent.getDriverId() != null) deletedEvent.put("driverId", parent.getDriverId());
        if (parent.getRiderId() != null) deletedEvent.put("riderId", parent.getRiderId());
        ratingEventPublisher.publishRatingDeleted(deletedEvent);
        ratingEventPublisher.publishRatingUpdated(deletedEvent);
    }

}
