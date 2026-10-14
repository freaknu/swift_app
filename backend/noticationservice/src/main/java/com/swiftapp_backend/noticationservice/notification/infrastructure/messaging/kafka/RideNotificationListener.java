package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.application.port.ProfileContactPort;
import com.swiftapp_backend.noticationservice.notification.application.service.EmailDeliveryService;
import com.swiftapp_backend.noticationservice.notification.application.service.NotificationService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Consumer;

/** Sends lifecycle mail to the profile IDs actually present in rider events. */
@Component
@RequiredArgsConstructor
public class RideNotificationListener {
    private final NotificationService notificationService;
    private final ProfileContactPort contacts;
    private final EmailDeliveryService deliveries;

    @KafkaListener(topics = {"ride.requested", "ride-requested"}, groupId = "notification-service")
    public void handleRideRequested(ConsumerRecord<String, Map<String, Object>> record,
                                    Acknowledgment acknowledgment) {
        // Requests have no assigned driver. The targeted, expiring offer uses push.
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = {"ride.assigned", "ride-assigned", "driver.assigned"},
            groupId = "notification-service")
    public void handleRideAccepted(ConsumerRecord<String, Map<String, Object>> record,
                                   Acknowledgment acknowledgment) {
        Map<String, Object> event = record.value();
        if (valid(event, record)) {
            String driverName = name(contact(id(event, "driverId"), UserRole.DRIVER), "your driver");
            String vehicle = text(event, "rideType", "shown in the app");
            email(event, UserRole.RIDER, "DRIVER_ASSIGNED", c ->
                    notificationService.sendRideAcceptedNotification(
                            c.email(), name(c, "rider"), driverName, vehicle));
        }
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = {"driver.arrived", "driver-arrived"}, groupId = "notification-service")
    public void handleDriverArrived(ConsumerRecord<String, Map<String, Object>> record,
                                    Acknowledgment acknowledgment) {
        Map<String, Object> event = record.value();
        if (valid(event, record)) {
            String driverName = name(contact(id(event, "driverId"), UserRole.DRIVER), "your driver");
            email(event, UserRole.RIDER, "DRIVER_ARRIVED", c ->
                    notificationService.sendDriverArrivedNotification(c.email(), name(c, "rider"), driverName));
        }
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = {"ride.started", "ride-started"}, groupId = "notification-service")
    public void handleRideStarted(ConsumerRecord<String, Map<String, Object>> record,
                                  Acknowledgment acknowledgment) {
        Map<String, Object> event = record.value();
        if (valid(event, record)) {
            email(event, UserRole.RIDER, "STARTED", c ->
                    notificationService.sendRideStartedNotification(
                            c.email(), name(c, "rider"), text(event, "destination", "shown in the app")));
        }
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = {"ride.completed", "ride-completed"}, groupId = "notification-service")
    public void handleRideCompleted(ConsumerRecord<String, Map<String, Object>> record,
                                    Acknowledgment acknowledgment) {
        Map<String, Object> event = record.value();
        if (valid(event, record)) {
            Double fare = event.get("totalFare") instanceof Number number ? number.doubleValue() : null;
            email(event, UserRole.RIDER, "COMPLETED", c ->
                    notificationService.sendRideCompletedNotification(c.email(), name(c, "rider"), fare));
        }
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = {"ride.cancelled", "ride-cancelled"}, groupId = "notification-service")
    public void handleRideCancelled(ConsumerRecord<String, Map<String, Object>> record,
                                    Acknowledgment acknowledgment) {
        Map<String, Object> event = record.value();
        if (valid(event, record)) {
            String cancelledBy = text(event, "cancelledByType", "the trip participant");
            String reason = text(event, "reason", "No reason provided");
            email(event, UserRole.RIDER, "CANCELLED", c ->
                    notificationService.sendRideCancelledNotification(
                            c.email(), name(c, "rider"), cancelledBy, reason));
            if (id(event, "driverId") != null) {
                email(event, UserRole.DRIVER, "CANCELLED", c ->
                        notificationService.sendRideCancelledNotification(
                                c.email(), name(c, "driver"), cancelledBy, reason));
            }
        }
        acknowledgment.acknowledge();
    }

    private void email(Map<String, Object> event, UserRole role, String type,
                       Consumer<ProfileContactPort.Contact> send) {
        Long profileId = id(event, role == UserRole.RIDER ? "riderId" : "driverId");
        Long rideId = id(event, "rideId");
        if (profileId == null || rideId == null) return;
        ProfileContactPort.Contact contact = contact(profileId, role);
        if (contact == null || contact.email() == null || contact.email().isBlank()) return;
        if (Boolean.FALSE.equals(contact.emailNotificationEnabled())) {
            // Record the opt-out, so an alias received after settings change cannot mail an old event.
            deliveries.deliver(type + ":" + rideId, profileId, role, () -> {});
            return;
        }
        deliveries.deliver(type + ":" + rideId, profileId, role, () -> send.accept(contact));
    }

    private ProfileContactPort.Contact contact(Long id, UserRole role) {
        return id == null ? null : contacts.find(id, role);
    }

    private boolean valid(Map<String, Object> event, ConsumerRecord<?, ?> record) {
        if (event == null || id(event, "rideId") == null) return false;
        LocalDateTime occurred;
        try {
            occurred = LocalDateTime.parse(String.valueOf(event.get("eventTime")));
        } catch (RuntimeException ignored) {
            occurred = record.timestamp() > 0
                    ? LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault())
                    : LocalDateTime.now();
        }
        // Historical broker backlog should populate the inbox but must not mail months-old rides.
        return !occurred.isBefore(LocalDateTime.now().minusMinutes(15));
    }

    private static Long id(Map<String, Object> event, String field) {
        try {
            long id = Long.parseLong(String.valueOf(event.get(field)));
            return id > 0 ? id : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String name(ProfileContactPort.Contact contact, String fallback) {
        return contact == null || contact.fullName() == null || contact.fullName().isBlank()
                ? fallback : contact.fullName();
    }

    private static String text(Map<String, Object> event, String field, String fallback) {
        Object value = event.get(field);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }
}
