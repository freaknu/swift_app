package com.swiftapp_backend.noticationservice.notification.application.service;

/** The outcome of an attempted push to one recipient's registered devices. */
public enum PushDeliveryResult {
    SENT,
    NO_DEVICES,
    NO_VALID_DEVICES,
    PREFERENCES_DISABLED,
    PUSH_DISABLED,
    INVALID_RECIPIENT
}
