package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.application.service.PushNotificationService;
import com.swiftapp_backend.noticationservice.notification.application.service.NotificationInboxService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;

class PushRideListenerTest {
    @Test
    void sendsBothAliasTopicsToSameDurableRecipientEventKeys() {
        PushNotificationService push = mock(PushNotificationService.class);
        NotificationInboxService inbox = mock(NotificationInboxService.class);
        PushRideListener listener = new PushRideListener(push, inbox);
        Acknowledgment ack = mock(Acknowledgment.class);
        Map<String, Object> event = Map.of(
                "rideId", 101L, "riderId", 42L, "driverId", 11L,
                "rideStatus", "DRIVER_ASSIGNED", "eventTime", "2026-10-09T19:00:00");

        listener.onRideEvent(record("ride.assigned", event), ack);
        listener.onRideEvent(record("ride-assigned", event), ack);

        verify(inbox, times(2)).deliver(eq("DRIVER_ASSIGNED:101"), eq(101L), eq("DRIVER_ASSIGNED"),
                eq(42L), eq(UserRole.RIDER), eq("Your driver is on the way"),
                eq("Your driver is on the way. Open Swift for trip details."),
                any(LocalDateTime.class), eq(Map.of("rideId", "101", "type", "DRIVER_ASSIGNED")),
                eq(300L), eq(true));
        verify(inbox, times(2)).deliver(eq("DRIVER_ASSIGNED:101"), eq(101L), eq("DRIVER_ASSIGNED"),
                eq(11L), eq(UserRole.DRIVER), eq("Ride accepted"),
                eq("The ride is assigned to you. Open Swift for trip details."),
                any(LocalDateTime.class), eq(Map.of("rideId", "101", "type", "DRIVER_ASSIGNED")),
                eq(300L), eq(true));
        verifyNoInteractions(push);
        verify(ack, times(2)).acknowledge();
    }

    private ConsumerRecord<String, Map<String, Object>> record(String topic, Map<String, Object> value) {
        return new ConsumerRecord<>(topic, 0, 0L, "101", value);
    }
}
