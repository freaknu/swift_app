package com.swiftapp_backend.userservice.user.infrastructure.persistence.adapter;

import com.swiftapp_backend.userservice.user.domain.model.RideHistoryModel;
import com.swiftapp_backend.userservice.user.domain.repository.RideHistoryRepository;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper.RideHistoryPersistenceMapper;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.repository.SpringDataRideHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RideHistoryRepositoryAdapter implements RideHistoryRepository {
    private final SpringDataRideHistoryRepository repository;
    private final RideHistoryPersistenceMapper mapper;

    @Override
    public RideHistoryModel save(RideHistoryModel rideHistory) {
        return mapper.toDomain(repository.save(mapper.toEntity(rideHistory)));
    }

    @Override
    public List<RideHistoryModel> findByAuthUserIdOrderByCreatedAtDesc(Long authUserId) {
        return repository.findByAuthUserIdOrderByCreatedAtDesc(authUserId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RideHistoryModel> findByAuthUserIdInOrderByCreatedAtDesc(List<Long> authUserIds) {
        return repository.findByAuthUserIdInOrderByCreatedAtDesc(authUserIds).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
