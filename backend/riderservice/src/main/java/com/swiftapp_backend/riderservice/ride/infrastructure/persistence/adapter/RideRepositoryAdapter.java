package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.adapter;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.model.PageResult;
import com.swiftapp_backend.riderservice.ride.domain.model.RecentLocation;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper.RidePersistenceMapper;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.RideLocationQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RideRepositoryAdapter implements RideRepository {
    private final JpaRideRepository repository;
    private final RideLocationQueryRepository locationQueryRepository;
    private final RidePersistenceMapper mapper;

    @Override
    @Transactional
    public RideModel save(RideModel ride) {
        return mapper.toDomain(repository.save(mapper.toEntity(ride)));
    }

    @Override
    public Optional<RideModel> findById(Long rideId) {
        return repository.findById(rideId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<RideModel> findByIdForUpdate(Long rideId) {
        return repository.findByIdForUpdate(rideId).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(Long rideId) {
        return repository.existsById(rideId);
    }

    @Override
    public boolean existsByRiderIdAndCurrentStatusIn(
            Long riderId, Collection<RideStatus> statuses) {
        return repository.existsByRiderIdAndCurrentStatusIn(riderId, statuses);
    }

    @Override
    public boolean existsByDriverIdAndCurrentStatusIn(
            Long driverId, Collection<RideStatus> statuses) {
        return repository.existsByDriverIdAndCurrentStatusIn(driverId, statuses);
    }

    @Override
    public Optional<RideModel> findFirstByRiderIdAndCurrentStatusInOrderByRequestedAtDesc(
            Long riderId, Collection<RideStatus> statuses) {
        return repository.findFirstByRiderIdAndCurrentStatusInOrderByRequestedAtDesc(riderId, statuses)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RideModel> findFirstByDriverIdAndCurrentStatusInOrderByRequestedAtDesc(
            Long driverId, Collection<RideStatus> statuses) {
        return repository.findFirstByDriverIdAndCurrentStatusInOrderByRequestedAtDesc(driverId, statuses)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<RideModel> findAllByRiderIdOrderByRequestedAtDesc(
            Long riderId, int page, int size) {
        Page<RideEntity> result = repository.findAllByRiderIdOrderByRequestedAtDesc(
                riderId, PageRequest.of(page, size));
        List<RideModel> content = result.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override
    public PageResult<RideModel> findAllByDriverIdOrderByRequestedAtDescIdDesc(
            Long driverId, int page, int size) {
        Page<RideEntity> result = repository.findAllByDriverIdOrderByRequestedAtDescIdDesc(
                driverId, PageRequest.of(page, size));
        List<RideModel> content = result.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override
    public PageResult<RideModel> findAllByDriverIdAndCurrentStatusOrderByRequestedAtDescIdDesc(
            Long driverId, com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus status,
            int page, int size) {
        Page<RideEntity> result = repository.findAllByDriverIdAndCurrentStatusOrderByRequestedAtDescIdDesc(
                driverId, status, PageRequest.of(page, size));
        List<RideModel> content = result.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override
    public List<String> getRideTypeCodesByUsage(Long riderId) {
        return repository.getRideTypeCodesByUsage(riderId);
    }

    @Override
    public List<RecentLocation> findPreviousDestinations(
            Long riderId, double latitude, double longitude, double radiusMeters) {
        return locationQueryRepository.findPreviousDestinations(
                        riderId, latitude, longitude, radiusMeters)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
