package com.swiftapp_backend.userservice.user.domain.repository;

import com.swiftapp_backend.userservice.user.domain.model.UserPreferencesModel;

import java.util.Optional;

public interface UserPreferencesRepository {
    Optional<UserPreferencesModel> findByUserId(Long userId);
    UserPreferencesModel save(UserPreferencesModel model);
}
