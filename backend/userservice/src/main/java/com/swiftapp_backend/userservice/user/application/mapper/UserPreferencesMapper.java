package com.swiftapp_backend.userservice.user.application.mapper;

import com.swiftapp_backend.userservice.user.api.dto.UserPreferencesDto;
import com.swiftapp_backend.userservice.user.domain.model.UserPreferencesModel;
import org.springframework.stereotype.Component;

@Component
public class UserPreferencesMapper {

    public UserPreferencesDto toDto(UserPreferencesModel model) {
        if (model == null) return null;
        return UserPreferencesDto.builder()
                .language(model.getLanguage())
                .theme(model.getTheme())
                .pushNotificationEnabled(model.isPushNotificationEnabled())
                .emailNotificationEnabled(model.isEmailNotificationEnabled())
                .smsNotificationEnabled(model.isSmsNotificationEnabled())
                .preferredPaymentMethod(model.getPreferredPaymentMethod())
                .build();
    }

    public UserPreferencesModel toModel(UserPreferencesDto dto, Long userId) {
        if (dto == null) return null;
        return UserPreferencesModel.builder()
                .userId(userId)
                .language(dto.getLanguage())
                .theme(dto.getTheme())
                .pushNotificationEnabled(dto.isPushNotificationEnabled())
                .emailNotificationEnabled(dto.isEmailNotificationEnabled())
                .smsNotificationEnabled(dto.isSmsNotificationEnabled())
                .preferredPaymentMethod(dto.getPreferredPaymentMethod())
                .build();
    }
}
