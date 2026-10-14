package com.swiftapp_backend.noticationservice.notification.domain.repository;

import com.swiftapp_backend.noticationservice.notification.domain.model.UserFcmToken;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;

public interface UserFcmRepository extends JpaRepository<UserFcmToken, Long> {
    List<UserFcmToken> findByUserIdAndRoleAndActiveTrueAndNotificationEnabledTrue(Long userId, UserRole role);
    Optional<UserFcmToken> findByInstallationId(String installationId);
    Optional<UserFcmToken> findByFcmToken(String fcmToken);
    void deleteByInstallationIdAndAuthUserId(String installationId, Long authUserId);
}
