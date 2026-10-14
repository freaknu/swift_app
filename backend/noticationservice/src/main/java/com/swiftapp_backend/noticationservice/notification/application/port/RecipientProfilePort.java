package com.swiftapp_backend.noticationservice.notification.application.port;

import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;

/** Resolve a caller's rider/driver profile ID from their authenticated user ID. */
public interface RecipientProfilePort {
    Long profileId(Long authUserId, UserRole role);
}
