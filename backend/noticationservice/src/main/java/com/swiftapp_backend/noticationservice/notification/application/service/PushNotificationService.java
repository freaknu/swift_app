package com.swiftapp_backend.noticationservice.notification.application.service;

import com.google.firebase.messaging.*;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.repository.UserFcmRepository;
import com.swiftapp_backend.noticationservice.notification.application.port.ProfileContactPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import com.swiftapp_backend.noticationservice.notification.domain.model.UserFcmToken;

@Service
@Slf4j
@RequiredArgsConstructor
public class PushNotificationService {
    private static final Set<String> RIDE_ALERT_TYPES = Set.of("RIDE_OFFER", "DRIVER_ASSIGNED",
            "DRIVER_ARRIVED", "STARTED", "COMPLETED", "CANCELLED", "MATCH_FAILED");
    private final ObjectProvider<FirebaseMessaging> messaging;
    private final UserFcmRepository tokens;
    private final ProfileContactPort contacts;

    public PushDeliveryResult send(Long recipientId, UserRole role, String title, String body,
                                   Map<String, String> data, long ttlSeconds) {
        return sendInternal(recipientId, role, title, body, data, ttlSeconds, true);
    }

    /**
     * Chat uses Android data messages for native inline replies. Apple receives
     * an APNs alert so chat visibility does not depend on a silent background task.
     */
    public PushDeliveryResult sendDataOnly(Long recipientId, UserRole role, String title, String body,
                                           Map<String, String> data, long ttlSeconds) {
        return sendInternal(recipientId, role, title, body, data, ttlSeconds, false);
    }

    private PushDeliveryResult sendInternal(Long recipientId, UserRole role, String title, String body,
                                            Map<String, String> data, long ttlSeconds,
                                            boolean includeNotification) {
        if (recipientId == null || recipientId <= 0 || role == null || ttlSeconds <= 0) {
            return PushDeliveryResult.INVALID_RECIPIENT;
        }
        FirebaseMessaging client = messaging.getIfAvailable();
        if (client == null) {
            log.info("Push skipped: Firebase messaging disabled for {} profile {}", role, recipientId);
            return PushDeliveryResult.PUSH_DISABLED;
        }
        List<UserFcmToken> devices = tokens.findByUserIdAndRoleAndActiveTrueAndNotificationEnabledTrue(recipientId, role);
        if (devices.isEmpty()) {
            log.info("Push skipped: no registered devices for {} profile {}", role, recipientId);
            return PushDeliveryResult.NO_DEVICES;
        }
        ProfileContactPort.Contact contact = contacts.find(recipientId, role);
        if (contact == null) {
            log.warn("Push skipped: missing contact for {} profile {}", role, recipientId);
            return PushDeliveryResult.INVALID_RECIPIENT;
        }
        Map<String, String> safeData = data == null ? Map.of() : data;
        if (Boolean.FALSE.equals(contact.pushNotificationEnabled())
                || (RIDE_ALERT_TYPES.contains(safeData.getOrDefault("type", ""))
                    && Boolean.FALSE.equals(contact.rideAlertsEnabled()))) {
            log.info("Push skipped: preference disabled for {} profile {}", role, recipientId);
            return PushDeliveryResult.PREFERENCES_DISABLED;
        }
        RuntimeException failure = null;
        int sent = 0;
        int invalid = 0;
        for (var device : devices) {
            if (device.getAuthUserId() == null || device.getAuthUserId() <= 0 ||
                    device.getFcmToken() == null || device.getFcmToken().isBlank()) {
                invalid++;
                log.warn("Push skipped: incomplete registration for device {}", device.getId());
                continue;
            }
            Map<String, String> payload = new HashMap<>(safeData);
            payload.put("authUserId", device.getAuthUserId().toString());
            payload.put("role", role.name());
            String identity = safeData.getOrDefault("eventId", safeData.getOrDefault("messageId",
                    safeData.getOrDefault("rideId", "general")));
            // APNs collapse identifiers are limited to 64 bytes. Keep separate
            // chat messages/reviews visible and retain stable ride replacements.
            String tag = UUID.nameUUIDFromBytes((safeData.getOrDefault("type", "notification")
                    + ":" + identity).getBytes(StandardCharsets.UTF_8)).toString();
            AndroidConfig.Builder android = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setTtl(ttlSeconds * 1000);
            if (includeNotification) {
                android.setNotification(AndroidNotification.builder().setChannelId("swift_rides")
                        .setIcon("ic_stat_notification").setSound("default").setTag(tag).build());
            }
            Message.Builder messageBuilder = Message.builder().setToken(device.getFcmToken())
                    .putAllData(payload)
                    .setAndroidConfig(android.build());
            if (includeNotification) {
                messageBuilder.setNotification(Notification.builder().setTitle(title).setBody(body).build());
            }
            boolean chatAlert = "CHAT_MESSAGE".equals(safeData.get("type"));
            boolean appleAlert = includeNotification || chatAlert;
            Aps.Builder aps = Aps.builder().setContentAvailable(!appleAlert);
            if (appleAlert) aps.setSound("default");
            if (chatAlert) {
                aps.setAlert(ApsAlert.builder().setTitle(title).setBody(body).build())
                        .setCategory("swift_chat");
                String sessionId = safeData.get("sessionId");
                if (sessionId != null && !sessionId.isBlank()) aps.setThreadId("chat:" + sessionId);
            }
            messageBuilder.setApnsConfig(ApnsConfig.builder()
                    .putHeader("apns-priority", appleAlert ? "10" : "5")
                    .putHeader("apns-push-type", appleAlert ? "alert" : "background")
                    .putHeader("apns-expiration", Long.toString(Instant.now().getEpochSecond() + ttlSeconds))
                    .putHeader("apns-collapse-id", tag)
                    .setAps(aps.build()).build());
            Message message = messageBuilder.build();
            try {
                client.send(message);
                sent++;
            } catch (FirebaseMessagingException ex) {
                if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    tokens.deleteById(device.getId());
                    invalid++;
                } else {
                    // Continue to the account's other devices. Propagate transient errors
                    // to Kafka so a network failure doesn't silently drop this event.
                    log.warn("FCM delivery failed for device {}: {}", device.getId(), ex.getMessagingErrorCode());
                    failure = new IllegalStateException("Push delivery failed", ex);
                }
            }
        }
        if (sent > 0) {
            if (failure != null) {
                log.warn("Push reached {} device(s) for {} profile {}, but another device failed",
                        sent, role, recipientId);
            }
            return PushDeliveryResult.SENT;
        }
        if (failure != null) throw failure;
        log.info("Push skipped: no valid devices for {} profile {} ({} invalid)", role, recipientId, invalid);
        return PushDeliveryResult.NO_VALID_DEVICES;
    }
}
