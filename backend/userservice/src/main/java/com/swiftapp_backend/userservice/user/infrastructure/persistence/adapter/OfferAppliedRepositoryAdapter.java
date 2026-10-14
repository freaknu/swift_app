package com.swiftapp_backend.userservice.user.infrastructure.persistence.adapter;

import com.swiftapp_backend.userservice.user.domain.model.AppliedOfferTrackModel;
import com.swiftapp_backend.userservice.user.domain.repository.OfferAppliedRepository;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper.AppliedOfferTrackPersistenceMapper;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.repository.SpringDataOfferAppliedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OfferAppliedRepositoryAdapter implements OfferAppliedRepository {
    private final SpringDataOfferAppliedRepository repository;
    private final AppliedOfferTrackPersistenceMapper mapper;

    @Override
    public AppliedOfferTrackModel save(AppliedOfferTrackModel offer) {
        return mapper.toDomain(repository.save(mapper.toEntity(offer)));
    }

    @Override
    public List<AppliedOfferTrackModel> findAllByAuthUserId(Long authUserId) {
        return repository.findAllByAuthUserId(authUserId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AppliedOfferTrackModel> findAllByOfferId(Long offerId) {
        return repository.findAllByOfferId(offerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AppliedOfferTrackModel> findAllByOfferIdAndAuthUserId(Long offerId, Long authUserId) {
        return repository.findAllByOfferIdAndAuthUserId(offerId, authUserId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Integer countByOfferIdAndAuthUserId(Long offerId, Long authUserId) {
        return repository.countByOfferIdAndAuthUserId(offerId, authUserId);
    }
}
