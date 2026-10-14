package com.swiftapp_backend.noticationservice.notification.domain.model;

public record EmailNotification(String recipient, String subject, String htmlContent) {
}
