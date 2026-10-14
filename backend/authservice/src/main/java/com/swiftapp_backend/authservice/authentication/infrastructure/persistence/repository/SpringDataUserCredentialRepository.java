package com.swiftapp_backend.authservice.authentication.infrastructure.persistence.repository;

import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.entity.UserCredentialsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserCredentialRepository extends JpaRepository<UserCredentialsEntity, Long> {
    List<UserCredentialsEntity> findAllByUserId(Long userId);

    Optional<UserCredentialsEntity> findByRefreshToken(String refreshToken);

    Optional<UserCredentialsEntity> findByAccessToken(String accessToken);

    void deleteAllByUserId(Long userId);
}
