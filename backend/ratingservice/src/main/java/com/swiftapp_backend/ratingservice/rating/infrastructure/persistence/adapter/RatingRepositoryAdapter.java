package com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.adapter;

import com.swiftapp_backend.ratingservice.rating.domain.model.RatingModel;
import com.swiftapp_backend.ratingservice.rating.domain.repository.RatingRepository;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.mapper.RatingPersistenceMapper;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.repository.SpringDataRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingRepositoryAdapter implements RatingRepository {
    private final SpringDataRatingRepository repository;
    private final RatingPersistenceMapper mapper;

    @Override
    public Optional<RatingModel> findByDriverId(Long driverId) {
        return repository.findByDriverId(driverId).map(mapper::toDomain);
    }

    @Override
    public Optional<RatingModel> findByRiderId(Long riderId) {
        return repository.findByRiderId(riderId).map(mapper::toDomain);
    }

    @Override
    public Optional<RatingModel> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public RatingModel save(RatingModel rating) {
        return mapper.toDomain(repository.save(mapper.toEntity(rating)));
    }
}
