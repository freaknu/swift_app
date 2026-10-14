package com.swiftapp_backend.userservice.user.infrastructure.persistence.repository;

import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.UserPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserPreferencesRepository extends JpaRepository<UserPreferencesEntity, Long> {
    Optional<UserPreferencesEntity> findByUserId(Long userId);
}
