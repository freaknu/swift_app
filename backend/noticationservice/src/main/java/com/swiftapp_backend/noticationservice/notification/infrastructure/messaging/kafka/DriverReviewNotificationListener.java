package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.application.port.ProfileContactPort;
import com.swiftapp_backend.noticationservice.notification.application.service.NotificationInboxService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Durable driver-facing review and account alerts, without routine profile update noise. */
@Component
@Slf4j
@RequiredArgsConstructor
public class DriverReviewNotificationListener {
    private static final long PUSH_FRESHNESS_SECONDS = 900;
    private final NotificationInboxService inbox;
    private final ProfileContactPort contacts;

    @KafkaListener(topics = {"driver.document.reviewed", "driver.vehicle.reviewed",
            "driver.account.status.changed"}, groupId = "notification-driver-status-v1",
            properties = "auto.offset.reset=latest")
    public void onReviewOrAccountEvent(ConsumerRecord<String, Map<String, Object>> record,
                                       Acknowledgment acknowledgment) {
        Map<String, Object> event = record.value();
        Event alert = event == null ? null : alert(record.topic(), event);
        if (alert == null) {
            acknowledgment.acknowledge();
            return;
        }

        ProfileContactPort.Contact contact = contacts.find(alert.driverId(), UserRole.DRIVER);
        if (contact == null || !alert.driverId().equals(contact.id())
                || !UserRole.DRIVER.name().equals(contact.role())
                || !alert.authUserId().equals(contact.authUserId())) {
            log.warn("Ignoring driver alert with unmatched recipient for profile {}", alert.driverId());
            acknowledgment.acknowledge();
            return;
        }

        Instant occurred = occurredAt(event.get("eventTime"), record.timestamp());
        Map<String, String> data = new HashMap<>();
        data.put("type", alert.type());
        data.put("eventId", alert.eventId());
        data.put("driverId", alert.driverId().toString());
        if (alert.documentId() != null) data.put("documentId", alert.documentId().toString());
        if (alert.vehicleId() != null) data.put("vehicleId", alert.vehicleId().toString());

        inbox.deliver(alert.type() + ":" + alert.eventId(), null, alert.type(),
                alert.driverId(), UserRole.DRIVER, alert.title(), alert.body(),
                LocalDateTime.ofInstant(occurred, ZoneId.systemDefault()), data,
                300, isFresh(occurred));
        acknowledgment.acknowledge();
    }

    private Event alert(String topic, Map<String, Object> event) {
        String eventId = eventId(event.get("eventId"));
        Long driverId = id(event.get("driverId"));
        Long authUserId = id(event.get("authUserId"));
        if (eventId == null || driverId == null || authUserId == null) {
            log.warn("Ignoring incomplete driver review/account event on {}", topic);
            return null;
        }

        String status = text(event.get("status"));
        if (status == null) return null;
        String reason = shortReason(event.get("reason"));
        return switch (topic) {
            case "driver.document.reviewed" -> documentAlert(
                    eventId, driverId, authUserId, id(event.get("documentId")),
                    text(event.get("documentType")), status, reason);
            case "driver.vehicle.reviewed" -> vehicleAlert(
                    eventId, driverId, authUserId, id(event.get("vehicleId")), status);
            case "driver.account.status.changed" -> accountAlert(
                    eventId, driverId, authUserId, status, reason);
            default -> null;
        };
    }

    private Event documentAlert(String eventId, Long driverId, Long authUserId,
                                Long documentId, String documentType, String status,
                                String reason) {
        if (documentId == null || documentType == null) return null;
        String name = documentType.replace('_', ' ').toLowerCase();
        return switch (status) {
            case "APPROVED" -> new Event(eventId, driverId, authUserId, documentId, null,
                    "DOCUMENT_APPROVED", "Document approved",
                    "Your " + name + " was approved. Open Documents for details.");
            case "REJECTED" -> new Event(eventId, driverId, authUserId, documentId, null,
                    "DOCUMENT_REJECTED", "Document needs changes",
                    "Your " + name + " was rejected." + reasonSuffix(reason, " Open Documents to replace it."));
            case "PENDING", "UNDER_REVIEW" -> new Event(eventId, driverId, authUserId, documentId, null,
                    "DOCUMENT_PENDING", "Document under review",
                    "Your " + name + " is under review. Open Documents for details.");
            default -> null;
        };
    }

    private Event vehicleAlert(String eventId, Long driverId, Long authUserId,
                               Long vehicleId, String status) {
        if (vehicleId == null) return null;
        return switch (status) {
            case "VERIFIED", "APPROVED" -> new Event(eventId, driverId, authUserId, null, vehicleId,
                    "VEHICLE_VERIFIED", "Vehicle verified",
                    "Your vehicle is verified. Open Vehicles for details.");
            case "REJECTED" -> new Event(eventId, driverId, authUserId, null, vehicleId,
                    "VEHICLE_REJECTED", "Vehicle needs changes",
                    "Your vehicle was rejected. Open Vehicles for details.");
            case "PENDING" -> new Event(eventId, driverId, authUserId, null, vehicleId,
                    "VEHICLE_PENDING", "Vehicle under review",
                    "Your vehicle is under review. Open Vehicles for details.");
            default -> null;
        };
    }

    private Event accountAlert(String eventId, Long driverId, Long authUserId,
                               String status, String reason) {
        return switch (status) {
            case "SUSPENDED" -> new Event(eventId, driverId, authUserId, null, null,
                    "DRIVER_SUSPENDED", "Driver account suspended",
                    "Your driver account has been suspended." + reasonSuffix(reason,
                            " Open Swift for details."));
            case "REACTIVATED" -> new Event(eventId, driverId, authUserId, null, null,
                    "DRIVER_REACTIVATED", "Driver account reactivated",
                    "Your driver account has been reactivated. Open Swift to get started.");
            default -> null;
        };
    }

    private static String reasonSuffix(String reason, String fallback) {
        return reason == null ? fallback : " Reason: " + reason;
    }

    private static String shortReason(Object value) {
        String reason = text(value);
        if (reason == null) return null;
        reason = reason.replaceAll("[\\p{Cntrl}]", " ").trim();
        if (reason.isEmpty()) return null;
        if (reason.equalsIgnoreCase("No review details provided.")) return null;
        return reason.length() > 160 ? reason.substring(0, 160) : reason;
    }

    private static String text(Object value) {
        return value instanceof CharSequence sequence && !sequence.toString().isBlank()
                ? sequence.toString() : null;
    }

    private static Long id(Object value) {
        try {
            long parsed = Long.parseLong(String.valueOf(value));
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String eventId(Object value) {
        try {
            return UUID.fromString(String.valueOf(value)).toString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Instant occurredAt(Object value, long recordTimestamp) {
        try {
            return Instant.parse(String.valueOf(value));
        } catch (RuntimeException ignored) {
            return recordTimestamp > 0 ? Instant.ofEpochMilli(recordTimestamp) : Instant.EPOCH;
        }
    }

    private static boolean isFresh(Instant occurred) {
        Instant now = Instant.now();
        return !occurred.isBefore(now.minusSeconds(PUSH_FRESHNESS_SECONDS))
                && !occurred.isAfter(now.plusSeconds(300));
    }

    private record Event(String eventId, Long driverId, Long authUserId,
                         Long documentId, Long vehicleId, String type, String title, String body) {}
}
