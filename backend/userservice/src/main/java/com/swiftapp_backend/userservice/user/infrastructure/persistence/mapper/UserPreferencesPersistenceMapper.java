package com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper;

import com.swiftapp_backend.userservice.user.domain.model.UserPreferencesModel;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.UserPreferencesEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPreferencesPersistenceMapper {

    public UserPreferencesModel toDomain(UserPreferencesEntity entity) {
        if (entity == null) return null;
        return UserPreferencesModel.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .language(entity.getLanguage())
                .theme(entity.getTheme())
                .pushNotificationEnabled(entity.isPushNotificationEnabled())
                .emailNotificationEnabled(entity.isEmailNotificationEnabled())
                .smsNotificationEnabled(entity.isSmsNotificationEnabled())
                .preferredPaymentMethod(entity.getPreferredPaymentMethod())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public UserPreferencesEntity toEntity(UserPreferencesModel model) {
        if (model == null) return null;
        return UserPreferencesEntity.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .language(model.getLanguage())
                .theme(model.getTheme())
                .pushNotificationEnabled(model.isPushNotificationEnabled())
                .emailNotificationEnabled(model.isEmailNotificationEnabled())
                .smsNotificationEnabled(model.isSmsNotificationEnabled())
                .preferredPaymentMethod(model.getPreferredPaymentMethod())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
