package com.swiftapp_backend.userservice.user.domain.repository;

import com.swiftapp_backend.userservice.user.domain.model.UserModel;

import java.util.List;
import java.util.Optional;

public interface UserModelRepository {
    List<UserModel> findAll();

    Optional<UserModel> findById(Long id);

    Optional<UserModel> findByEmail(String email);

    Optional<UserModel> findByAuthUserId(Long userId);

    UserModel save(UserModel user);
}
