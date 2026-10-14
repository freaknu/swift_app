package com.swiftapp_backend.authservice.authentication.infrastructure.persistence.adapter;

import com.swiftapp_backend.authservice.authentication.domain.model.UserCredentials;
import com.swiftapp_backend.authservice.authentication.domain.repository.UserCredentialRepository;
import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.mapper.UserCredentialsPersistenceMapper;
import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.repository.SpringDataUserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCredentialRepositoryAdapter implements UserCredentialRepository {
    private final SpringDataUserCredentialRepository repository;
    private final UserCredentialsPersistenceMapper mapper;

    @Override
    public List<UserCredentials> findAllByUserId(Long userId) {
        return repository.findAllByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<UserCredentials> findByRefreshToken(String refreshToken) {
        return repository.findByRefreshToken(refreshToken).map(mapper::toDomain);
    }

    @Override
    public Optional<UserCredentials> findByAccessToken(String accessToken) {
        return repository.findByAccessToken(accessToken).map(mapper::toDomain);
    }

    @Override
    public UserCredentials save(UserCredentials credentials) {
        return mapper.toDomain(repository.save(mapper.toEntity(credentials)));
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        repository.deleteAllByUserId(userId);
    }
}
