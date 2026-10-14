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
import java.util.Map;

/** Rating aggregates change after both new ratings and rating removals. */
@Component
@Slf4j
@RequiredArgsConstructor
public class RatingInboxNotificationListener {
    private final NotificationInboxService inbox;
    private final ProfileContactPort contacts;

    @KafkaListener(topics = "rating.updated", groupId = "notification-rating-inbox-v1",
            properties = "auto.offset.reset=latest")
    public void onRatingUpdated(ConsumerRecord<String, Map<String, Object>> record,
                                Acknowledgment acknowledgment) {
        Map<String, Object> event = record.value();
        Long ratingId = event == null ? null : positiveId(event.get("ratingId"));
        Long driverId = event == null ? null : positiveId(event.get("driverId"));
        Long riderId = event == null ? null : positiveId(event.get("riderId"));
        Double average = event == null ? null : average(event.get("averageRating"));
        if (!"rating.updated".equals(record.topic()) || ratingId == null || average == null
                || (driverId == null) == (riderId == null)) {
            log.warn("Ignoring incomplete or ambiguous rating update at offset {}", record.offset());
            acknowledgment.acknowledge();
            return;
        }

        UserRole role = driverId != null ? UserRole.DRIVER : UserRole.RIDER;
        Long recipientId = driverId != null ? driverId : riderId;
        ProfileContactPort.Contact contact = contacts.find(recipientId, role);
        if (contact == null || !recipientId.equals(contact.id())
                || !role.name().equals(contact.role())) {
            acknowledgment.acknowledge();
            return;
        }

        Instant occurred = record.timestamp() > 0
                ? Instant.ofEpochMilli(record.timestamp()) : Instant.EPOCH;
        String eventKey = "RATING_UPDATED:" + record.partition() + ":" + record.offset()
                + ":" + role + ":" + recipientId;
        inbox.deliver(eventKey, null, "RATING_UPDATED", recipientId, role,
                "Your rating was updated",
                "Open Ratings to view your current score.",
                LocalDateTime.ofInstant(occurred, ZoneId.systemDefault()),
                Map.of("type", "RATING_UPDATED", "eventId", eventKey, "ratingId", ratingId.toString(),
                        "averageRating", average.toString()), 300,
                !occurred.isBefore(Instant.now().minusSeconds(900))
                        && !occurred.isAfter(Instant.now().plusSeconds(300)));
        acknowledgment.acknowledge();
    }

    private static Long positiveId(Object value) {
        try {
            long parsed = Long.parseLong(String.valueOf(value));
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Double average(Object value) {
        if (!(value instanceof Number number)) return null;
        double score = number.doubleValue();
        return Double.isFinite(score) && score >= 0 && score <= 5 ? score : null;
    }
}
