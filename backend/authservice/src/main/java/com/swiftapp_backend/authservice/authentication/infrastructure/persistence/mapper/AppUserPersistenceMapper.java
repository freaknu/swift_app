package com.swiftapp_backend.authservice.authentication.infrastructure.persistence.mapper;

import com.swiftapp_backend.authservice.authentication.domain.model.AppUser;
import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.entity.AppUserEntity;
import org.springframework.stereotype.Component;

@Component
public class AppUserPersistenceMapper {

    public AppUser toDomain(AppUserEntity entity) {
        if (entity == null) {
            return null;
        }

        return AppUser.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .accountLocked(entity.isAccountLocked())
                .isActive(entity.isActive())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AppUserEntity toEntity(AppUser model) {
        if (model == null) {
            return null;
        }

        return AppUserEntity.builder()
                .id(model.getId())
                .name(model.getName())
                .email(model.getEmail())
                .accountLocked(model.isAccountLocked())
                .isActive(model.isActive())
                .role(model.getRole())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
