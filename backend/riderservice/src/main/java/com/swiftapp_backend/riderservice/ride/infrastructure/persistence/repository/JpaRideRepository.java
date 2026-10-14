package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaRideRepository extends JpaRepository<RideEntity,Long> {
    Page<RideEntity> findAllByRiderIdOrderByRequestedAtDesc(Long riderId, Pageable pageable);

    Page<RideEntity> findAllByDriverIdOrderByRequestedAtDescIdDesc(Long driverId, Pageable pageable);

    Page<RideEntity> findAllByDriverIdAndCurrentStatusOrderByRequestedAtDescIdDesc(
            Long driverId, RideStatus status, Pageable pageable);

    boolean existsByRiderIdAndCurrentStatusIn(Long riderId, Collection<RideStatus> statuses);

    boolean existsByDriverIdAndCurrentStatusIn(Long driverId, Collection<RideStatus> statuses);

    Optional<RideEntity> findFirstByRiderIdAndCurrentStatusInOrderByRequestedAtDesc(
            Long riderId, Collection<RideStatus> statuses);

    Optional<RideEntity> findFirstByDriverIdAndCurrentStatusInOrderByRequestedAtDesc(
            Long driverId, Collection<RideStatus> statuses);

    @Query("""
            select count(r) from RideModel r
            where r.offer.id = :offerId
              and (:riderId is null or r.riderId = :riderId)
              and r.currentStatus not in (
                com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus.CANCELLED,
                com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus.MATCH_FAILED)
            """)
    long countOfferUses(@Param("offerId") Long offerId, @Param("riderId") Long riderId);

    boolean existsByRiderIdAndCurrentStatus(Long riderId, RideStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RideModel r where r.id = :rideId")
    Optional<RideEntity> findByIdForUpdate(@Param("rideId") Long rideId);


    @Query("""
    SELECT r.rideTypeCode
    FROM RideModel r
    WHERE r.riderId = :authUserId
      AND r.rideTypeCode IS NOT NULL
    GROUP BY r.rideTypeCode
    ORDER BY COUNT(r.id) DESC
""")
    List<String> getRideTypeCodesByUsage(@Param("authUserId") Long authUserId);
}
