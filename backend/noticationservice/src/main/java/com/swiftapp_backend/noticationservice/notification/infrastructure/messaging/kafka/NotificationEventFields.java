package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import java.util.Map;

/** Reads optional fields from events published by independently deployed services. */
final class NotificationEventFields {
    private NotificationEventFields() {}

    static String text(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof CharSequence text ? text.toString() : null;
    }

    static Double amount(Map<String, Object> data, String key, String fallbackKey) {
        Object value = data.get(key);
        if (value == null && fallbackKey != null) value = data.get(fallbackKey);
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
