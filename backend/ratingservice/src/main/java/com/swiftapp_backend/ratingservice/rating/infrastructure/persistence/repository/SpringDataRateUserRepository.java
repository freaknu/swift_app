package com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.repository;

import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity.RateUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataRateUserRepository extends JpaRepository<RateUserEntity, Long> {
    List<RateUserEntity> findAllByRating_Id(Long ratingId);
    Optional<RateUserEntity> findByRideIdAndRaterAuthUserId(Long rideId, Long authUserId);
}
