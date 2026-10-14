package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.application.port.ProfileContactPort;
import com.swiftapp_backend.noticationservice.notification.application.service.NotificationInboxService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.infrastructure.config.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RatingInboxNotificationListenerTest {
    private final NotificationInboxService inbox = mock(NotificationInboxService.class);
    private final ProfileContactPort contacts = mock(ProfileContactPort.class);
    private final RatingInboxNotificationListener listener =
            new RatingInboxNotificationListener(inbox, contacts);
    private final Acknowledgment ack = mock(Acknowledgment.class);

    @Test
    void notifiesDriverForHeaderlessRatingAggregateUpdateWithStableOffsetKey() {
        contact(23L, UserRole.DRIVER);
        ConsumerRecord<String, Map<String, Object>> record = record("rating.updated", 44L,
                Instant.now(), """
                        {"ratingId":9,"driverId":23,"averageRating":4.25,"rating":4.25}
                        """);

        listener.onRatingUpdated(record, ack);
        listener.onRatingUpdated(record, ack);

        verify(inbox, times(2)).deliver(eq("RATING_UPDATED:2:44:DRIVER:23"), isNull(),
                eq("RATING_UPDATED"), eq(23L), eq(UserRole.DRIVER),
                eq("Your rating was updated"), anyString(), any(LocalDateTime.class),
                eq(Map.of("type", "RATING_UPDATED", "eventId", "RATING_UPDATED:2:44:DRIVER:23",
                        "ratingId", "9",
                        "averageRating", "4.25")), eq(300L), eq(true));
        verify(ack, times(2)).acknowledge();
    }

    @Test
    void notifiesRiderAndStoresOlderChangeWithoutLatePush() {
        contact(31L, UserRole.RIDER);
        listener.onRatingUpdated(record("rating.updated", 45L,
                Instant.now().minusSeconds(3600), """
                        {"ratingId":10,"riderId":31,"averageRating":5.0,"rating":5.0}
                        """), ack);

        verify(inbox).deliver(eq("RATING_UPDATED:2:45:RIDER:31"), isNull(),
                eq("RATING_UPDATED"), eq(31L), eq(UserRole.RIDER),
                eq("Your rating was updated"), anyString(), any(LocalDateTime.class),
                eq(Map.of("type", "RATING_UPDATED", "eventId", "RATING_UPDATED:2:45:RIDER:31",
                        "ratingId", "10",
                        "averageRating", "5.0")), eq(300L), eq(false));
        verify(ack).acknowledge();
    }

    @Test
    void ignoresAmbiguousOrInvalidRecipientAndAliasRecords() {
        listener.onRatingUpdated(record("rating.updated", 46L, Instant.now(), """
                {"ratingId":9,"driverId":23,"riderId":31,"averageRating":4.25}
                """), ack);
        listener.onRatingUpdated(record("rating.updated", 47L, Instant.now(), """
                {"ratingId":9,"driverId":23,"averageRating":6.0}
                """), ack);
        listener.onRatingUpdated(record("rating-updated", 48L, Instant.now(), """
                {"ratingId":9,"driverId":23,"averageRating":4.25}
                """), ack);

        verifyNoInteractions(inbox, contacts);
        verify(ack, times(3)).acknowledge();
    }

    @Test
    void missingRecipientProfileDoesNotCreateAnInboxRow() {
        listener.onRatingUpdated(record("rating.updated", 49L, Instant.now(), """
                {"ratingId":9,"driverId":23,"averageRating":4.25}
                """), ack);

        verifyNoInteractions(inbox);
        verify(ack).acknowledge();
    }

    private void contact(Long recipientId, UserRole role) {
        when(contacts.find(recipientId, role)).thenReturn(new ProfileContactPort.Contact(
                recipientId, 8L, "Recipient", "recipient@example.com", role.name(), true, true));
    }

    private static ConsumerRecord<String, Map<String, Object>> record(
            String topic, long offset, Instant timestamp, String json) {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "127.0.0.1:1");
        JsonDeserializer<Map<String, Object>> deserializer = new JsonDeserializer<>();
        deserializer.configure(config.consumerFactory().getConfigurationProperties(), false);
        try {
            return new ConsumerRecord<>(topic, 2, offset, timestamp.toEpochMilli(),
                    TimestampType.CREATE_TIME, -1, -1, "rating9",
                    deserializer.deserialize(topic, json.getBytes(StandardCharsets.UTF_8)),
                    new RecordHeaders(), Optional.empty());
        } finally {
            deserializer.close();
        }
    }
}
