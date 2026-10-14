package com.swiftapp_backend.noticationservice.notification.application.port;

import com.swiftapp_backend.noticationservice.notification.domain.model.Notification;

public interface NotificationPort {
     void sendNotification(Notification notification);
}
