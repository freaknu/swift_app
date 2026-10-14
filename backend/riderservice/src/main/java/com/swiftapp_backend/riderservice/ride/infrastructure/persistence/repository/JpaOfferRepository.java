package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JpaOfferRepository extends JpaRepository<OfferEntity, Long> {

    List<OfferEntity> findByActiveTrue();

    List<OfferEntity> findByRideTypeCodeAndActiveTrue(String rideTypeCode);

    List<OfferEntity> findByRideTypeCodeIsNullAndActiveTrue();

    List<OfferEntity> findByRideTypeCodeAndActiveTrueOrderByCreatedAtDesc(
            String rideTypeCode
    );

    List<OfferEntity> findByActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
            LocalDateTime now1,
            LocalDateTime now2
    );

    List<OfferEntity> findByRideTypeCodeAndActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
            String rideTypeCode,
            LocalDateTime now1,
            LocalDateTime now2
    );

    OfferEntity findByOfferCodeAndActiveTrue(String offerCode);

    boolean existsByOfferCode(String offerCode);
}
