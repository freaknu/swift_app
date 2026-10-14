package com.swiftapp_backend.noticationservice.notification.domain.repository;

import com.swiftapp_backend.noticationservice.notification.domain.model.Notification;

import java.util.List;

public interface NotificationRepository {
    List<Notification> getAllRiderMessages(Long userId);
    List<Notification> getAllDriverMessages(Long userId);
    void markAsReadNotification(Long userId, Long messageId);
    void deleteNotification(Long userId, Long messageId);
}
