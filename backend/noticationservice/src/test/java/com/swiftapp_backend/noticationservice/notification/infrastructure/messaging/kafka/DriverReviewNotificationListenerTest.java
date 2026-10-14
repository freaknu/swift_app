package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.application.port.ProfileContactPort;
import com.swiftapp_backend.noticationservice.notification.application.service.NotificationInboxService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.infrastructure.config.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DriverReviewNotificationListenerTest {
    private static final String EVENT_ID = "c1808050-95da-42b8-a969-c2923fccbd99";
    private final NotificationInboxService inbox = mock(NotificationInboxService.class);
    private final ProfileContactPort contacts = mock(ProfileContactPort.class);
    private final DriverReviewNotificationListener listener =
            new DriverReviewNotificationListener(inbox, contacts);
    private final Acknowledgment ack = mock(Acknowledgment.class);

    @Test
    void documentReviewUsesProfileRecipientAndStableEventIdForHeaderlessKafkaValue() {
        driverContact(23L, 8L);
        String json = """
                {"eventId":"%s","driverId":23,"authUserId":8,"status":"REJECTED",
                 "eventTime":"%s","documentId":9,"documentType":"DRIVING_LICENSE",
                 "reason":"Photo is blurry"}
                """.formatted(EVENT_ID, Instant.now());

        listener.onReviewOrAccountEvent(record("driver.document.reviewed", json), ack);
        listener.onReviewOrAccountEvent(record("driver.document.reviewed", json), ack);

        verify(inbox, times(2)).deliver(eq("DOCUMENT_REJECTED:" + EVENT_ID), isNull(),
                eq("DOCUMENT_REJECTED"), eq(23L), eq(UserRole.DRIVER),
                eq("Document needs changes"),
                eq("Your driving license was rejected. Reason: Photo is blurry"),
                any(LocalDateTime.class),
                eq(Map.of("type", "DOCUMENT_REJECTED", "eventId", EVENT_ID,
                        "driverId", "23", "documentId", "9")),
                eq(300L), eq(true));
        verify(ack, times(2)).acknowledge();
    }

    @Test
    void vehicleVerificationAndAccountChangesProduceUsefulDriverAlerts() {
        driverContact(23L, 8L);
        String now = Instant.now().toString();

        listener.onReviewOrAccountEvent(record("driver.vehicle.reviewed", """
                {"eventId":"%s","driverId":23,"authUserId":8,"status":"VERIFIED",
                 "eventTime":"%s","vehicleId":17}
                """.formatted(EVENT_ID, now)), ack);
        listener.onReviewOrAccountEvent(record("driver.account.status.changed", """
                {"eventId":"ca428c46-c5c5-4431-bded-cd64e4e1b8ba","driverId":23,
                 "authUserId":8,"status":"SUSPENDED","eventTime":"%s",
                 "reason":"Expired insurance"}
                """.formatted(now)), ack);
        listener.onReviewOrAccountEvent(record("driver.account.status.changed", """
                {"eventId":"ea206cf7-63b1-4332-aa3b-ac87eaefc810","driverId":23,
                 "authUserId":8,"status":"REACTIVATED","eventTime":"%s"}
                """.formatted(now)), ack);

        verify(inbox).deliver(eq("VEHICLE_VERIFIED:" + EVENT_ID), isNull(),
                eq("VEHICLE_VERIFIED"), eq(23L), eq(UserRole.DRIVER),
                eq("Vehicle verified"), anyString(), any(LocalDateTime.class),
                eq(Map.of("type", "VEHICLE_VERIFIED", "eventId", EVENT_ID,
                        "driverId", "23", "vehicleId", "17")),
                eq(300L), eq(true));
        verify(inbox).deliver(startsWith("DRIVER_SUSPENDED:"), isNull(),
                eq("DRIVER_SUSPENDED"), eq(23L), eq(UserRole.DRIVER),
                eq("Driver account suspended"),
                eq("Your driver account has been suspended. Reason: Expired insurance"),
                any(LocalDateTime.class), anyMap(), eq(300L), eq(true));
        verify(inbox).deliver(startsWith("DRIVER_REACTIVATED:"), isNull(),
                eq("DRIVER_REACTIVATED"), eq(23L), eq(UserRole.DRIVER),
                eq("Driver account reactivated"), anyString(),
                any(LocalDateTime.class), anyMap(), eq(300L), eq(true));
        verify(ack, times(3)).acknowledge();
    }

    @Test
    void historicalReviewPersistsInInboxWithoutDevicePush() {
        driverContact(23L, 8L);
        listener.onReviewOrAccountEvent(record("driver.document.reviewed", """
                {"eventId":"%s","driverId":23,"authUserId":8,"status":"APPROVED",
                 "eventTime":"%s","documentId":9,"documentType":"DRIVING_LICENSE"}
                """.formatted(EVENT_ID, Instant.now().minusSeconds(3600))), ack);

        verify(inbox).deliver(eq("DOCUMENT_APPROVED:" + EVENT_ID), isNull(),
                eq("DOCUMENT_APPROVED"), eq(23L), eq(UserRole.DRIVER),
                eq("Document approved"), anyString(), any(LocalDateTime.class),
                anyMap(), eq(300L), eq(false));
        verify(ack).acknowledge();
    }

    @Test
    void rejectsForeignProfileAndRoutineUpdateEventsWithoutInboxEntry() {
        when(contacts.find(23L, UserRole.DRIVER)).thenReturn(new ProfileContactPort.Contact(
                23L, 99L, "Driver", "driver@example.com", "DRIVER", true, true));
        String json = """
                {"eventId":"%s","driverId":23,"authUserId":8,"status":"APPROVED",
                 "eventTime":"%s","documentId":9,"documentType":"DRIVING_LICENSE"}
                """.formatted(EVENT_ID, Instant.now());

        listener.onReviewOrAccountEvent(record("driver.document.reviewed", json), ack);
        listener.onReviewOrAccountEvent(record("driver.vehicle.updated", json), ack);

        verifyNoInteractions(inbox);
        verify(ack, times(2)).acknowledge();
    }

    @Test
    void failedInboxWriteLeavesRecordUnacknowledgedForRetry() {
        driverContact(23L, 8L);
        doThrow(new IllegalStateException("database temporarily unavailable"))
                .when(inbox).deliver(anyString(), isNull(), anyString(), eq(23L),
                        eq(UserRole.DRIVER), anyString(), anyString(),
                        any(LocalDateTime.class), anyMap(), eq(300L), anyBoolean());
        String json = """
                {"eventId":"%s","driverId":23,"authUserId":8,"status":"VERIFIED",
                 "eventTime":"%s","vehicleId":17}
                """.formatted(EVENT_ID, Instant.now());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> listener.onReviewOrAccountEvent(record("driver.vehicle.reviewed", json), ack));
        verifyNoInteractions(ack);
    }

    private void driverContact(Long driverId, Long authUserId) {
        when(contacts.find(driverId, UserRole.DRIVER)).thenReturn(new ProfileContactPort.Contact(
                driverId, authUserId, "Driver", "driver@example.com", "DRIVER", true, true));
    }

    private static ConsumerRecord<String, Map<String, Object>> record(String topic, String json) {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "127.0.0.1:1");
        JsonDeserializer<Map<String, Object>> deserializer = new JsonDeserializer<>();
        deserializer.configure(config.consumerFactory().getConfigurationProperties(), false);
        try {
            return new ConsumerRecord<>(topic, 0, 42L, "driver8",
                    deserializer.deserialize(topic, json.getBytes(StandardCharsets.UTF_8)));
        } finally {
            deserializer.close();
        }
    }
}
