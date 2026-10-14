package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.application.port.DriverNotificationSettingsPort;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverNotificationSettings;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverNotificationPreferencesEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverNotificationPreferenceRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverNotificationSettingsAdapter implements DriverNotificationSettingsPort {
    private final SpringDataDriverNotificationPreferenceRepository preferences;
    private final SpringDataDriverRepository drivers;

    @Override
    public Optional<DriverNotificationSettings> find(Long driverId, Long authUserId) {
        return preferences.findByDriver_Id(driverId).map(entity -> {
            requireOwner(entity, driverId, authUserId);
            return toSettings(entity);
        });
    }

    @Override
    @Transactional
    public DriverNotificationSettings save(DriverNotificationSettings settings) {
        if (settings == null || settings.driverId() == null || settings.authUserId() == null) {
            throw new IllegalArgumentException("Driver profile is required for notification settings");
        }
        // Serialize first-write migration and concurrent PUTs for one driver.
        DriverEntity driver = drivers.findByIdForNotificationUpdate(settings.driverId())
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found"));
        if (!settings.authUserId().equals(driver.getAuthUserId())) {
            throw new IllegalArgumentException("Notification settings must belong to this driver");
        }
        DriverNotificationPreferencesEntity entity = preferences.findByDriver_Id(settings.driverId())
                .orElseGet(DriverNotificationPreferencesEntity::new);
        if (entity.getId() == null) {
            preferences.findByAuthUserId(settings.authUserId()).ifPresent(other -> {
                if (other.getDriver() == null
                        || !settings.driverId().equals(other.getDriver().getId())) {
                    throw new IllegalStateException("Notification settings have another driver association");
                }
            });
        } else {
            requireOwner(entity, settings.driverId(), settings.authUserId());
        }
        entity.setDriver(driver);
        entity.setAuthUserId(settings.authUserId());
        entity.setPushEnabled(settings.pushEnabled());
        entity.setSmsEnabled(settings.smsEnabled());
        entity.setEmailEnabled(settings.emailEnabled());
        entity.setRideRequestEnabled(settings.rideAlertsEnabled());
        entity.setPromotionEnabled(settings.surgeAlertsEnabled());
        return toSettings(preferences.saveAndFlush(entity));
    }

    private static void requireOwner(DriverNotificationPreferencesEntity entity,
                                     Long driverId, Long authUserId) {
        if (entity.getDriver() == null || !driverId.equals(entity.getDriver().getId())
                || (entity.getAuthUserId() != null && !authUserId.equals(entity.getAuthUserId()))
                || !authUserId.equals(entity.getDriver().getAuthUserId())) {
            throw new IllegalStateException("Notification settings have another driver association");
        }
    }

    private static DriverNotificationSettings toSettings(DriverNotificationPreferencesEntity entity) {
        return new DriverNotificationSettings(entity.getDriver().getId(), entity.getAuthUserId(),
                entity.isPushEnabled(), entity.isSmsEnabled(), entity.isEmailEnabled(),
                entity.isRideRequestEnabled(), entity.isPromotionEnabled());
    }
}
