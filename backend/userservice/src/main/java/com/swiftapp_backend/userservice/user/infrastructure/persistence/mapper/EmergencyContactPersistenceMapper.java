package com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper;

import com.swiftapp_backend.userservice.user.domain.model.EmergencyContactModel;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.EmergencyContactEntity;
import org.springframework.stereotype.Component;

@Component
public class EmergencyContactPersistenceMapper {

    public EmergencyContactModel toDomain(EmergencyContactEntity entity) {
        if (entity == null) {
            return null;
        }

        return EmergencyContactModel.builder()
                .id(entity.getId())
                .contactType(entity.getContactType())
                .contactPersonName(entity.getContactPersonName())
                .contactPersonNumber(entity.getContactPersonNumber())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public EmergencyContactEntity toEntity(EmergencyContactModel model) {
        if (model == null) {
            return null;
        }

        return EmergencyContactEntity.builder()
                .id(model.getId())
                .contactType(model.getContactType())
                .contactPersonName(model.getContactPersonName())
                .contactPersonNumber(model.getContactPersonNumber())
                .isActive(model.isActive())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
