package com.swiftapp_backend.noticationservice.notification.api.dto;

import java.time.LocalDateTime;

public record InboxNotificationDto(Long id, Long rideId, Long sessionId, String messageId,
                                   String type, String title,
                                   String body, boolean read, LocalDateTime createdAt) {}
