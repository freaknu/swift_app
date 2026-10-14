package com.swiftapp_backend.userservice.user.infrastructure.persistence.repository;

import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserModelRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByAuthUserId(Long userId);
}
