package com.swiftapp_backend.authservice.authentication.domain.repository;

import com.swiftapp_backend.authservice.authentication.domain.model.UserCredentials;

import java.util.List;
import java.util.Optional;

public interface UserCredentialRepository {
    List<UserCredentials> findAllByUserId(Long userId);

    Optional<UserCredentials> findByRefreshToken(String refreshToken);

    Optional<UserCredentials> findByAccessToken(String accessToken);

    UserCredentials save(UserCredentials credentials);

    void deleteAllByUserId(Long userId);
}
