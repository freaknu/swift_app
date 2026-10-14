package com.swiftapp_backend.noticationservice.notification.application.mapper;

import com.swiftapp_backend.noticationservice.notification.api.dto.OtpSendNotificationDto;
import com.swiftapp_backend.noticationservice.notification.domain.model.EmailNotification;

public final class NotificationMapper {
    private NotificationMapper() {
    }

    public static EmailNotification toDomain(OtpSendNotificationDto dto) {
        return new EmailNotification(dto.getEmail(), dto.getSubject(), dto.getHtmlContent());
    }
}
