package com.swiftapp_backend.authservice.authentication.infrastructure.persistence.repository;

import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataAppUserRepository extends JpaRepository<AppUserEntity, Long> {
    Optional<AppUserEntity> findByEmail(String email);
}
