package com.swiftapp_backend.riderservice.ride.domain.repository;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.model.PageResult;
import com.swiftapp_backend.riderservice.ride.domain.model.RecentLocation;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RideRepository {
    RideModel save(RideModel ride);

    Optional<RideModel> findById(Long rideId);

    Optional<RideModel> findByIdForUpdate(Long rideId);

    boolean existsById(Long rideId);

    boolean existsByRiderIdAndCurrentStatusIn(Long riderId, Collection<RideStatus> statuses);

    boolean existsByDriverIdAndCurrentStatusIn(Long driverId, Collection<RideStatus> statuses);

    Optional<RideModel> findFirstByRiderIdAndCurrentStatusInOrderByRequestedAtDesc(
            Long riderId, Collection<RideStatus> statuses);

    Optional<RideModel> findFirstByDriverIdAndCurrentStatusInOrderByRequestedAtDesc(
            Long driverId, Collection<RideStatus> statuses);

    PageResult<RideModel> findAllByRiderIdOrderByRequestedAtDesc(
            Long riderId, int page, int size);

    PageResult<RideModel> findAllByDriverIdOrderByRequestedAtDescIdDesc(
            Long driverId, int page, int size);

    PageResult<RideModel> findAllByDriverIdAndCurrentStatusOrderByRequestedAtDescIdDesc(
            Long driverId, RideStatus status, int page, int size);

    List<String> getRideTypeCodesByUsage(Long riderId);

    List<RecentLocation> findPreviousDestinations(
            Long riderId, double latitude, double longitude, double radiusMeters);
}
