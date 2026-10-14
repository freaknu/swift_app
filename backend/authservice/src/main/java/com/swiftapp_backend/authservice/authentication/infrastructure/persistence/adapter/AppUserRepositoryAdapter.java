package com.swiftapp_backend.authservice.authentication.infrastructure.persistence.adapter;

import com.swiftapp_backend.authservice.authentication.domain.model.AppUser;
import com.swiftapp_backend.authservice.authentication.domain.repository.AppUserRepository;
import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.mapper.AppUserPersistenceMapper;
import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.repository.SpringDataAppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AppUserRepositoryAdapter implements AppUserRepository {
    private final SpringDataAppUserRepository repository;
    private final AppUserPersistenceMapper mapper;

    @Override
    public Optional<AppUser> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<AppUser> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public AppUser save(AppUser user) {
        return mapper.toDomain(repository.save(mapper.toEntity(user)));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
