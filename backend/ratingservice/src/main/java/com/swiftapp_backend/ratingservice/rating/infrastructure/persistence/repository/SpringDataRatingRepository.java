package com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.repository;

import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity.RatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataRatingRepository extends JpaRepository<RatingEntity, Long> {
    Optional<RatingEntity> findByDriverId(Long driverId);

    Optional<RatingEntity> findByRiderId(Long riderId);
}
