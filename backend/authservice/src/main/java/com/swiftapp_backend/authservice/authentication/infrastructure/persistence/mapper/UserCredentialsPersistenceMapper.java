package com.swiftapp_backend.authservice.authentication.infrastructure.persistence.mapper;

import com.swiftapp_backend.authservice.authentication.domain.model.UserCredentials;
import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.entity.UserCredentialsEntity;
import org.springframework.stereotype.Component;

@Component
public class UserCredentialsPersistenceMapper {

    public UserCredentials toDomain(UserCredentialsEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserCredentials.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .accessToken(entity.getAccessToken())
                .refreshToken(entity.getRefreshToken())
                .deviceName(entity.getDeviceName())
                .os(entity.getOs())
                .deviceModel(entity.getDeviceModel())
                .fingerPrint(entity.getFingerPrint())
                .ipAddress(entity.getIpAddress())
                .appLockPin(entity.getAppLockPin())
                .isAppLocked(entity.isAppLocked())
                .isLoggedOut(entity.isLoggedOut())
                .loggedInAt(entity.getLoggedInAt())
                .build();
    }

    public UserCredentialsEntity toEntity(UserCredentials model) {
        if (model == null) {
            return null;
        }

        return UserCredentialsEntity.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .accessToken(model.getAccessToken())
                .refreshToken(model.getRefreshToken())
                .deviceName(model.getDeviceName())
                .os(model.getOs())
                .deviceModel(model.getDeviceModel())
                .fingerPrint(model.getFingerPrint())
                .ipAddress(model.getIpAddress())
                .appLockPin(model.getAppLockPin())
                .isAppLocked(model.isAppLocked())
                .isLoggedOut(model.isLoggedOut())
                .loggedInAt(model.getLoggedInAt())
                .build();
    }
}
