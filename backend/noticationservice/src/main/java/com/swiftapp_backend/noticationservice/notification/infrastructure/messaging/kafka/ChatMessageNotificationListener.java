package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.application.service.NotificationInboxService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/** Sends data-only FCM pushes for persisted chat messages. */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChatMessageNotificationListener {
    private final NotificationInboxService inbox;

    @KafkaListener(
            topics = "chat.message.created",
            groupId = "notification-chat-push-v1",
            properties = "auto.offset.reset=latest")
    public void onMessage(ConsumerRecord<String, Map<String, Object>> record,
                          Acknowledgment acknowledgment) {
        Map<String, Object> event = record.value();
        if (event == null) {
            acknowledgment.acknowledge();
            return;
        }
        String messageId = text(event, "messageId");
        Long recipientId = id(event.get("recipientId"));
        UserRole role = role(text(event, "recipientRole"));
        if (messageId == null || recipientId == null || role == null) {
            log.warn("Ignoring chat notification without message recipient context");
            acknowledgment.acknowledge();
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("type", "CHAT_MESSAGE");
        put(data, "messageId", messageId);
        put(data, "sessionId", event.get("sessionId"));
        put(data, "rideId", event.get("rideId"));
        put(data, "riderId", event.get("riderId"));
        put(data, "driverId", event.get("driverId"));
        put(data, "replyOfMessageId", event.get("replyOfMessageId"));
        String body = text(event, "messageContent");
        if (body == null || body.isBlank()) body = "You received a new message.";
        body = body.length() > 180 ? body.substring(0, 177) + "..." : body;
        data.put("messageContent", body);
        data.put("title", "New ride message");
        data.put("body", body);

        Long rideId = id(event.get("rideId"));
        LocalDateTime occurredAt = record.timestamp() > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();
        boolean fresh = record.timestamp() <= 0
                || record.timestamp() >= Instant.now().minusSeconds(900).toEpochMilli();
        inbox.deliverDataOnly("CHAT_MESSAGE:" + messageId, rideId, "CHAT_MESSAGE",
                recipientId, role, "New ride message", body, occurredAt,
                data, 300, fresh);
        acknowledgment.acknowledge();
    }

    private static String text(Map<String, Object> event, String key) {
        Object value = event.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static void put(Map<String, String> data, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            data.put(key, String.valueOf(value));
        }
    }

    private static Long id(Object value) {
        try {
            long parsed = Long.parseLong(String.valueOf(value));
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static UserRole role(String value) {
        if (value == null) return null;
        try {
            return UserRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
