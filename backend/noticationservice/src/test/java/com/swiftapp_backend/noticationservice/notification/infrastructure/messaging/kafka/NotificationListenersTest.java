package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.api.dto.OtpSendNotificationDto;
import com.swiftapp_backend.noticationservice.notification.application.service.NotificationService;
import com.swiftapp_backend.noticationservice.notification.application.service.EmailDeliveryService;
import com.swiftapp_backend.noticationservice.notification.application.port.ProfileContactPort;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.infrastructure.config.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.*;

class NotificationListenersTest {
    private final NotificationService service = mock(NotificationService.class);
    private final ProfileContactPort contacts = mock(ProfileContactPort.class);
    private final EmailDeliveryService deliveries = mock(EmailDeliveryService.class);
    private final RideNotificationListener rides = new RideNotificationListener(service, contacts, deliveries);
    private final PaymentNotificationListener payments = new PaymentNotificationListener(service);
    private final RatingNotificationListener ratings = new RatingNotificationListener(service);

    @Test
    void resolvesCurrentRiderEventsByProfileIdAndAcknowledgesAfterSynchronousDelivery() {
        when(contacts.find(31L, UserRole.RIDER)).thenReturn(
                new ProfileContactPort.Contact(31L, 7L, "Rider", "rider@example.com", "RIDER", true, true));
        when(contacts.find(23L, UserRole.DRIVER)).thenReturn(
                new ProfileContactPort.Contact(23L, 8L, "Driver", "driver@example.com", "DRIVER", true, true));
        doAnswer(call -> {
            ((Runnable) call.getArgument(3)).run();
            return null;
        }).when(deliveries).deliver(anyString(), anyLong(), any(), any(Runnable.class));
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        rides.handleRideRequested(record("ride.requested", """
                {"rideId":17,"riderId":31,"driverId":null,"vehicleId":null,"rideStatus":"MATCHING",
                 "eventTime":"2026-10-08T19:30:00","rideType":"STANDARD","vehicleType":"SEDAN",
                 "pickupLatitude":12.9,"pickupLongitude":77.6,"destinationLatitude":13.0,
                 "destinationLongitude":77.7,"estimatedDistance":4.5,"estimatedDuration":600,
                 "totalFare":24.5,"currency":"INR"}
                """), acknowledgment);
        rides.handleRideAccepted(freshRecord("ride.assigned", """
                {"rideId":17,"riderId":31,"driverId":23,"vehicleId":9,
                 "rideStatus":"DRIVER_ASSIGNED","eventTime":"2026-10-08T19:31:00",
                 "acceptedAt":"2026-10-08T19:31:00"}
                """), acknowledgment);
        rides.handleDriverArrived(freshRecord("driver.arrived", """
                {"rideId":17,"riderId":31,"driverId":23,"vehicleId":9,
                 "rideStatus":"DRIVER_ARRIVED","eventTime":"2026-10-08T19:34:00",
                 "arrivedAt":"2026-10-08T19:34:00"}
                """), acknowledgment);
        rides.handleRideStarted(freshRecord("ride.started", """
                {"rideId":17,"riderId":31,"driverId":23,"vehicleId":9,
                 "rideStatus":"STARTED","eventTime":"2026-10-08T19:35:00",
                 "startedAt":"2026-10-08T19:35:00"}
                """), acknowledgment);
        ConsumerRecord<String, Map<String, Object>> completed = freshRecord("ride.completed", """
                {"rideId":17,"riderId":31,"driverId":23,"vehicleId":9,
                 "rideStatus":"COMPLETED","eventTime":"2026-10-08T19:45:00",
                 "actualDistance":4.5,"actualDuration":600,"totalFare":24.5,
                 "currency":"INR","completedAt":"2026-10-08T19:45:00"}
                """);
        rides.handleRideCompleted(completed, acknowledgment);
        rides.handleRideCancelled(freshRecord("ride.cancelled", """
                {"rideId":18,"riderId":31,"driverId":null,"vehicleId":null,
                 "rideStatus":"CANCELLED","eventTime":"2026-10-08T19:46:00",
                 "cancelledBy":31,"cancelledByType":"RIDER","reason":"No longer needed",
                 "cancelledAt":"2026-10-08T19:46:00"}
                """), acknowledgment);

        assertEquals(24.5,
                NotificationEventFields.amount(completed.value(), "totalFare", "fare"));
        verify(service).sendRideAcceptedNotification("rider@example.com", "Rider", "Driver", "shown in the app");
        verify(service).sendDriverArrivedNotification("rider@example.com", "Rider", "Driver");
        verify(service).sendRideStartedNotification("rider@example.com", "Rider", "shown in the app");
        verify(service).sendRideCompletedNotification("rider@example.com", "Rider", 24.5);
        verify(service).sendRideCancelledNotification("rider@example.com", "Rider", "RIDER", "No longer needed");
        verify(deliveries, times(1)).deliver(eq("CANCELLED:18"), eq(31L), eq(UserRole.RIDER), any());
        verify(acknowledgment, org.mockito.Mockito.times(6)).acknowledge();
    }

    @Test
    void consumesPaymentAndRatingRecordsWithHeaderlessMapValues() {
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        payments.handlePaymentCompleted(record("payment.completed", """
                {"riderEmail":"rider@example.com","riderName":"Rider","amount":24.5,"paymentMethod":"CARD"}
                """), acknowledgment);
        payments.handlePaymentFailed(record("payment.failed", """
                {"riderEmail":"rider@example.com","riderName":"Rider","amount":24.5,"reason":"Declined"}
                """), acknowledgment);
        ratings.handleRatingReminder(record("rating.reminder", """
                {"riderEmail":"rider@example.com","riderName":"Rider","driverName":"Driver"}
                """), acknowledgment);

        verify(service).sendPaymentSuccessNotification("rider@example.com", "Rider", 24.5, "CARD");
        verify(service).sendPaymentFailedNotification("rider@example.com", "Rider", 24.5, "Declined");
        verify(service).sendRatingReminderNotification("rider@example.com", "Rider", "Driver");
        verify(acknowledgment, org.mockito.Mockito.times(3)).acknowledge();
    }

    @Test
    void acknowledgesRideWithoutEmailAndSkipsNotification() {
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        rides.handleRideRequested(record("ride.requested", "{" +
                "\"rideId\":17,\"riderId\":31}"), acknowledgment);
        verifyNoInteractions(service, contacts, deliveries);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void respectsRiderEmailOptOutForActualRideEvent() {
        when(contacts.find(31L, UserRole.RIDER)).thenReturn(
                new ProfileContactPort.Contact(31L, 7L, "Rider", "rider@example.com",
                        "RIDER", true, false));
        doAnswer(call -> {
            ((Runnable) call.getArgument(3)).run();
            return null;
        }).when(deliveries).deliver(anyString(), anyLong(), any(), any(Runnable.class));
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        rides.handleRideStarted(freshRecord("ride.started",
                "{\"rideId\":17,\"riderId\":31,\"driverId\":23}"), acknowledgment);
        verify(deliveries).deliver(eq("STARTED:17"), eq(31L), eq(UserRole.RIDER), any());
        verifyNoInteractions(service);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void otpTopicKeepsItsDedicatedHeaderlessDtoDeserializer() {
        KafkaConsumerConfig config = config();
        Map<String, Object> properties = config.otpConsumerFactory().getConfigurationProperties();
        assertEquals(OtpSendNotificationDto.class.getName(),
                properties.get(JsonDeserializer.VALUE_DEFAULT_TYPE));
        JsonDeserializer<OtpSendNotificationDto> deserializer = new JsonDeserializer<>();
        deserializer.configure(properties, false);
        try {
            OtpSendNotificationDto dto = deserializer.deserialize("otp-notification",
                    "{\"email\":\"rider@example.com\",\"subject\":\"Code\",\"htmlContent\":\"1234\"}"
                            .getBytes(StandardCharsets.UTF_8));
            assertEquals("rider@example.com", dto.getEmail());
            assertEquals("Code", dto.getSubject());
        } finally {
            deserializer.close();
        }
    }

    private static ConsumerRecord<String, Map<String, Object>> record(String topic, String json) {
        KafkaConsumerConfig config = config();
        Map<String, Object> properties = config.consumerFactory().getConfigurationProperties();
        assertEquals("java.util.HashMap", properties.get(JsonDeserializer.VALUE_DEFAULT_TYPE));
        JsonDeserializer<Map<String, Object>> deserializer = new JsonDeserializer<>();
        deserializer.configure(properties, false);
        try {
            Map<String, Object> value = deserializer.deserialize(topic,
                    json.getBytes(StandardCharsets.UTF_8));
            return new ConsumerRecord<>(topic, 0, 0, "key", value);
        } finally {
            deserializer.close();
        }
    }

    private static ConsumerRecord<String, Map<String, Object>> freshRecord(String topic, String json) {
        ConsumerRecord<String, Map<String, Object>> record = record(topic, json);
        record.value().put("eventTime", LocalDateTime.now().toString());
        return record;
    }

    private static KafkaConsumerConfig config() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "127.0.0.1:1");
        return config;
    }
}
