package com.swiftapp_backend.authservice.authentication.domain.repository;

import com.swiftapp_backend.authservice.authentication.domain.model.AppUser;

import java.util.Optional;

public interface AppUserRepository {
    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findById(Long id);

    AppUser save(AppUser user);

    void deleteById(Long id);
}
