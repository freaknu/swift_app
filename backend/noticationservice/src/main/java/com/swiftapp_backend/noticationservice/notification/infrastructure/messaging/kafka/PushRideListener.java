package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.application.service.PushNotificationService;
import com.swiftapp_backend.noticationservice.notification.application.service.NotificationInboxService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PushRideListener {
    private final PushNotificationService push;
    private final NotificationInboxService inbox;

    @KafkaListener(topics = {"ride.offer.broadcast", "ride-offer-broadcast", "ride.assigned",
            "ride-assigned", "driver.assigned", "driver-assigned", "driver.arrived",
            "driver-arrived", "ride.started", "ride-started", "ride.completed",
            "ride-completed", "ride.cancelled", "ride-cancelled", "ride.match.failed",
            "ride-match-failed"},
            groupId = "notification-push-v1", properties = "auto.offset.reset=latest")
    public void onRideEvent(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment ack) {
        Map<String, Object> event = record.value();
        if (event == null) {
            ack.acknowledge();
            return;
        }
        String topic = record.topic().replace('-', '.');
        Long rideId = id(event.get("rideId"));
        if (rideId == null) {
            ack.acknowledge();
            return;
        }
        if (topic.equals("ride.offer.broadcast")) {
            if (isFresh(record)) offer(event, rideId);
        } else {
            String type = switch (topic) {
                case "ride.assigned", "driver.assigned" -> "DRIVER_ASSIGNED";
                case "driver.arrived" -> "DRIVER_ARRIVED";
                case "ride.started" -> "STARTED";
                case "ride.completed" -> "COMPLETED";
                case "ride.cancelled" -> "CANCELLED";
                default -> "MATCH_FAILED";
            };
            String title = switch (type) {
                case "DRIVER_ASSIGNED" -> "Your driver is on the way";
                case "DRIVER_ARRIVED" -> "Your driver has arrived";
                case "STARTED" -> "Your ride has started";
                case "COMPLETED" -> "Ride completed";
                case "CANCELLED" -> "Ride cancelled";
                default -> "No driver found";
            };
            Map<String, String> data = Map.of("rideId", rideId.toString(), "type", type);
            // Persist every lifecycle update even when an older Kafka backlog
            // should not produce a stale device alert.
            sendToParticipants(event, type, title, data, rideId,
                    occurredAt(event, record), isFresh(record));
        }
        // Exceptions escape to the Kafka error handler; never acknowledge a failed send.
        ack.acknowledge();
    }

    private void offer(Map<String, Object> event, Long rideId) {
        Instant expires;
        try {
            expires = Instant.parse(String.valueOf(event.get("expiresAt")));
        } catch (RuntimeException ex) {
            return; // An offer without a deadline cannot safely be displayed.
        }
        long ttl = expires.getEpochSecond() - Instant.now().getEpochSecond();
        if (ttl <= 0 || !(event.get("targets") instanceof List<?> targets)) return;
        Map<String, String> data = Map.of("rideId", rideId.toString(), "type", "RIDE_OFFER",
                "expiresAt", expires.toString());
        for (Object value : targets) {
            if (value instanceof Map<?, ?> target) {
                push.send(id(target.get("driverId")), UserRole.DRIVER, "New ride request",
                        "A rider nearby needs a pickup. Open Swift to review the request.", data, Math.min(ttl, 60));
            }
        }
    }

    private Long id(Object value) {
        try {
            long number = Long.parseLong(String.valueOf(value));
            return number > 0 ? number : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void sendToParticipants(Map<String, Object> event, String type, String riderTitle,
                                    Map<String, String> data, Long rideId,
                                    LocalDateTime occurredAt, boolean sendPush) {
        Long riderId = id(event.get("riderId"));
        Long driverId = id(event.get("driverId"));
        String riderBody = switch (type) {
            case "DRIVER_ASSIGNED" -> "Your driver is on the way. Open Swift for trip details.";
            case "DRIVER_ARRIVED" -> "Your driver has arrived. Open Swift for trip details.";
            case "STARTED" -> "Your ride has started. Have a safe journey.";
            case "COMPLETED" -> "Your ride is complete. Open Swift to view the receipt.";
            case "CANCELLED" -> "This trip is no longer active. Open Swift for the latest status.";
            default -> "Open Swift to view your trip details.";
        };
        String eventKey = type + ":" + rideId;
        inbox.deliver(eventKey, rideId, type, riderId, UserRole.RIDER,
                riderTitle, riderBody, occurredAt, data, 300, sendPush);

        if (driverId == null) return;
        String driverTitle = switch (type) {
            case "DRIVER_ASSIGNED" -> "Ride accepted";
            case "DRIVER_ARRIVED" -> "Arrival recorded";
            case "STARTED" -> "Ride started";
            case "COMPLETED" -> "Ride completed";
            case "CANCELLED" -> "Ride cancelled";
            default -> riderTitle;
        };
        String driverBody = switch (type) {
            case "DRIVER_ASSIGNED" -> "The ride is assigned to you. Open Swift for trip details.";
            case "DRIVER_ARRIVED" -> "Your arrival was recorded for this ride.";
            case "STARTED" -> "The ride is now in progress.";
            case "COMPLETED" -> "The ride has been completed.";
            case "CANCELLED" -> "This trip is no longer active. Open Swift for the latest status.";
            default -> "Open Swift to view trip details.";
        };
        inbox.deliver(eventKey, rideId, type, driverId, UserRole.DRIVER,
                driverTitle, driverBody, occurredAt, data, 300, sendPush);
    }

    private boolean isFresh(ConsumerRecord<?, ?> record) {
        return record.timestamp() <= 0
                || record.timestamp() >= Instant.now().minusSeconds(900).toEpochMilli();
    }

    private LocalDateTime occurredAt(Map<String, Object> event, ConsumerRecord<?, ?> record) {
        Object raw = event.get("eventTime");
        if (raw != null) {
            try {
                return LocalDateTime.parse(raw.toString());
            } catch (RuntimeException ignored) {
                // Older events may use another timestamp representation.
            }
        }
        return record.timestamp() > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();
    }
}
