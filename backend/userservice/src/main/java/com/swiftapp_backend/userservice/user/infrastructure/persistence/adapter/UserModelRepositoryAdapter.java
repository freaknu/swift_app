package com.swiftapp_backend.userservice.user.infrastructure.persistence.adapter;

import com.swiftapp_backend.userservice.user.domain.model.UserModel;
import com.swiftapp_backend.userservice.user.domain.repository.UserModelRepository;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.repository.SpringDataUserModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserModelRepositoryAdapter implements UserModelRepository {
    private final SpringDataUserModelRepository repository;
    private final UserPersistenceMapper mapper;

    @Override
    public List<UserModel> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<UserModel> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<UserModel> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<UserModel> findByAuthUserId(Long userId) {
        return repository.findByAuthUserId(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public UserModel save(UserModel user) {
        return mapper.toDomain(repository.save(mapper.toEntity(user)));
    }
}
