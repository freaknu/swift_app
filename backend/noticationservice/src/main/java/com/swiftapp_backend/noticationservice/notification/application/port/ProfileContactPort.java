package com.swiftapp_backend.noticationservice.notification.application.port;

import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;

public interface ProfileContactPort {
    Contact find(Long profileId, UserRole role);

    record Contact(Long id, Long authUserId, String fullName, String email, String role,
                   Boolean pushNotificationEnabled, Boolean emailNotificationEnabled,
                   Boolean rideAlertsEnabled) {
        public Contact(Long id, Long authUserId, String fullName, String email, String role,
                       Boolean pushNotificationEnabled, Boolean emailNotificationEnabled) {
            this(id, authUserId, fullName, email, role,
                    pushNotificationEnabled, emailNotificationEnabled, null);
        }
    }
}
