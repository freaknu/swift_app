package com.swiftapp_backend.noticationservice.notification.application.port;

import com.swiftapp_backend.noticationservice.notification.domain.model.EmailNotification;

public interface EmailSenderPort {
    void send(EmailNotification notification);
}
