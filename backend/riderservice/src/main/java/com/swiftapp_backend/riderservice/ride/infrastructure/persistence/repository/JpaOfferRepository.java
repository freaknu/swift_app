package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.OfferEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaOfferRepository extends JpaRepository<OfferEntity, Long> {

    List<OfferEntity> findByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OfferModel o where o.id = :offerId")
    Optional<OfferEntity> findByIdForUpdate(@Param("offerId") Long offerId);


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
