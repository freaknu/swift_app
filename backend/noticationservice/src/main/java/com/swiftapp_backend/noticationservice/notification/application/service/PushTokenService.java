package com.swiftapp_backend.noticationservice.notification.application.service;

import com.swiftapp_backend.noticationservice.notification.application.port.RecipientProfilePort;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.model.UserFcmToken;
import com.swiftapp_backend.noticationservice.notification.domain.repository.UserFcmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PushTokenService {
    private final UserFcmRepository repository;
    private final RecipientProfilePort recipientProfiles;

    @Transactional
    public void register(Long authUserId, String installationId, String token, UserRole role, boolean enabled) {
        if (authUserId == null || authUserId <= 0 || role == null || token == null
                || token.isBlank() || token.length() > 1024) {
            throw new IllegalArgumentException("A valid device token and role are required");
        }
        validateInstallation(installationId);
        // Never accept a rider/driver ID from the phone: resolve the authenticated
        // account's profile. Rider, driver and auth IDs use different sequences.
        long recipient = recipientProfiles.profileId(authUserId, role);
        repository.findByFcmToken(token).filter(old -> !installationId.equals(old.getInstallationId()))
                .ifPresent(old -> { repository.delete(old); repository.flush(); });
        UserFcmToken device = repository.findByInstallationId(installationId).orElseGet(UserFcmToken::new);
        device.setInstallationId(installationId);
        device.setAuthUserId(authUserId);
        device.setUserId(recipient);
        device.setRole(role);
        device.setFcmToken(token);
        device.setActive(true);
        device.setNotificationEnabled(enabled);
        if (device.getCreatedAt() == null) device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        repository.save(device);
    }

    @Transactional
    public void unregister(Long authUserId, String installationId) {
        validateInstallation(installationId);
        repository.deleteByInstallationIdAndAuthUserId(installationId, authUserId);
    }

    @Transactional(readOnly = true)
    public UserFcmToken ownedDevice(Long authUserId, String installationId) {
        validateInstallation(installationId);
        return repository.findByInstallationId(installationId)
                .filter(device -> authUserId != null && authUserId.equals(device.getAuthUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notification device is not registered for this account"));
    }

    private void validateInstallation(String id) {
        if (id == null || !id.matches("[a-zA-Z0-9-]{16,64}")) {
            throw new IllegalArgumentException("Invalid installation ID");
        }
    }
}
