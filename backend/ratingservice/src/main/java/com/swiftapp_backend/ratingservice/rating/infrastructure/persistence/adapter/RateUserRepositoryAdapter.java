package com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.adapter;

import com.swiftapp_backend.ratingservice.rating.domain.model.RateUserModel;
import com.swiftapp_backend.ratingservice.rating.domain.model.RatingModel;
import com.swiftapp_backend.ratingservice.rating.domain.repository.RateUserRepository;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity.RateUserEntity;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity.RatingEntity;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.mapper.RateUserPersistenceMapper;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.mapper.RatingPersistenceMapper;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.repository.SpringDataRateUserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateUserRepositoryAdapter implements RateUserRepository {
    private final SpringDataRateUserRepository repository;
    private final RateUserPersistenceMapper mapper;
    private final RatingPersistenceMapper ratingMapper;
    private final EntityManager entityManager;

    @Override
    public Optional<RateUserModel> findById(Long id) {
        return repository.findById(id).map(this::toDomainWithGraph);
    }

    @Override
    public Optional<RateUserModel> findByRideIdAndRaterAuthUserId(Long rideId, Long authUserId) {
        return repository.findByRideIdAndRaterAuthUserId(rideId, authUserId)
                .map(this::toDomainWithRatingReference);
    }

    @Override
    public List<RateUserModel> findAllByRating_Id(Long ratingId) {
        return repository.findAllByRating_Id(ratingId).stream()
                .map(this::toDomainWithRatingReference)
                .toList();
    }

    @Override
    @Transactional
    public RateUserModel save(RateUserModel rating) {
        RateUserEntity entity = mapper.toEntity(rating);
        if (rating.getRating() != null) {
            if (rating.getRating().getId() != null) {
                entity.setRating(entityManager.getReference(RatingEntity.class, rating.getRating().getId()));
            } else {
                entity.setRating(ratingMapper.toEntityReference(rating.getRating()));
            }
        }

        RateUserModel saved = mapper.toDomain(repository.save(entity));
        saved.setRating(rating.getRating());
        return saved;
    }

    private RateUserModel toDomainWithRatingReference(RateUserEntity entity) {
        RateUserModel model = mapper.toDomain(entity);
        if (entity.getRating() != null) {
            model.setRating(ratingMapper.toDomainReference(entity.getRating()));
        }
        return model;
    }

    private RateUserModel toDomainWithGraph(RateUserEntity entity) {
        if (entity.getRating() == null) {
            return mapper.toDomain(entity);
        }

        RatingModel parent = ratingMapper.toDomain(entity.getRating());
        if (parent.getAllRatings() != null) {
            for (RateUserModel candidate : parent.getAllRatings()) {
                if (Objects.equals(candidate.getId(), entity.getId())) {
                    return candidate;
                }
            }
        }

        RateUserModel model = mapper.toDomain(entity);
        model.setRating(parent);
        return model;
    }
}
