package com.swiftapp_backend.noticationservice.notification.application.service;

import com.swiftapp_backend.noticationservice.notification.api.dto.InboxNotificationDto;
import com.swiftapp_backend.noticationservice.notification.application.port.RecipientProfilePort;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.model.InboxNotification;
import com.swiftapp_backend.noticationservice.notification.domain.repository.InboxNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationInboxService {
    private final InboxNotificationRepository repository;
    private final RecipientProfilePort recipientProfiles;
    private final PushNotificationService push;

    /** Each recipient commits separately: a failed driver push cannot resend a
     * rider notification that already succeeded. Alias topics share an event key. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(String eventKey, Long rideId, String type, Long recipientId,
                        UserRole role, String title, String body, LocalDateTime occurredAt,
                        Map<String, String> data, long ttlSeconds, boolean sendPush) {
        deliverInternal(eventKey, rideId, type, recipientId, role, title, body,
                occurredAt, data, ttlSeconds, sendPush, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverDataOnly(String eventKey, Long rideId, String type, Long recipientId,
                                UserRole role, String title, String body, LocalDateTime occurredAt,
                                Map<String, String> data, long ttlSeconds, boolean sendPush) {
        deliverInternal(eventKey, rideId, type, recipientId, role, title, body,
                occurredAt, data, ttlSeconds, sendPush, true);
    }

    private void deliverInternal(String eventKey, Long rideId, String type, Long recipientId,
                                 UserRole role, String title, String body, LocalDateTime occurredAt,
                                 Map<String, String> data, long ttlSeconds, boolean sendPush,
                                 boolean dataOnly) {
        if (recipientId == null || recipientId <= 0 || role == null) return;
        InboxNotification notification = repository
                .findByEventKeyAndRecipientIdAndRecipientRole(eventKey, recipientId, role)
                .orElseGet(() -> {
                    InboxNotification created = new InboxNotification();
                    created.setEventKey(eventKey);
                    created.setRideId(rideId);
                    created.setSessionId(positiveId(data.get("sessionId")));
                    created.setMessageId(data.get("messageId"));
                    created.setType(type);
                    created.setRecipientId(recipientId);
                    created.setRecipientRole(role);
                    created.setTitle(title);
                    created.setBody(body);
                    created.setOccurredAt(occurredAt);
                    return repository.saveAndFlush(created);
                });
        if (notification.isPushDelivered() || !sendPush) return;
        PushDeliveryResult result = dataOnly
                ? push.sendDataOnly(recipientId, role, title, body, data, ttlSeconds)
                : push.send(recipientId, role, title, body, data, ttlSeconds);
        if (result == PushDeliveryResult.SENT) {
            notification.setPushDelivered(true);
            repository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public List<InboxNotificationDto> list(Long authUserId, UserRole role, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        Long recipientId = recipientProfiles.profileId(authUserId, role);
        return repository.findByRecipientIdAndRecipientRoleOrderByOccurredAtDescIdDesc(
                recipientId, role, PageRequest.of(0, limit)).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long authUserId, UserRole role) {
        Long recipientId = recipientProfiles.profileId(authUserId, role);
        return repository.countByRecipientIdAndRecipientRoleAndReadAtIsNull(recipientId, role);
    }

    @Transactional
    public InboxNotificationDto markRead(Long authUserId, UserRole role, Long notificationId) {
        Long recipientId = recipientProfiles.profileId(authUserId, role);
        InboxNotification notification = repository.findByIdAndRecipientIdAndRecipientRole(
                        notificationId, recipientId, role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            repository.save(notification);
        }
        return toDto(notification);
    }

    private InboxNotificationDto toDto(InboxNotification notification) {
        return new InboxNotificationDto(notification.getId(), notification.getRideId(),
                notification.getSessionId(), notification.getMessageId(),
                notification.getType(), notification.getTitle(), notification.getBody(),
                notification.getReadAt() != null, notification.getOccurredAt());
    }

    private static Long positiveId(String raw) {
        try {
            long value = Long.parseLong(raw);
            return value > 0 ? value : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
