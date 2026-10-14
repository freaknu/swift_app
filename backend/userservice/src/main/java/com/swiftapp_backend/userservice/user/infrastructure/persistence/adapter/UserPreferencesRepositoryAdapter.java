package com.swiftapp_backend.userservice.user.infrastructure.persistence.adapter;

import com.swiftapp_backend.userservice.user.domain.model.UserPreferencesModel;
import com.swiftapp_backend.userservice.user.domain.repository.UserPreferencesRepository;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper.UserPreferencesPersistenceMapper;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.repository.SpringDataUserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPreferencesRepositoryAdapter implements UserPreferencesRepository {

    private final SpringDataUserPreferencesRepository repository;
    private final UserPreferencesPersistenceMapper mapper;

    @Override
    public Optional<UserPreferencesModel> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public UserPreferencesModel save(UserPreferencesModel model) {
        return mapper.toDomain(repository.save(mapper.toEntity(model)));
    }
}
