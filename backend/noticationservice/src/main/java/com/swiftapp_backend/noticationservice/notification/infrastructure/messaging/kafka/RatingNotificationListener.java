package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RatingNotificationListener {
    private final NotificationService notificationService;

    @KafkaListener(
            topics = {"rating.reminder", "rating-reminder"},
            groupId = "notification-service"
    )
    public void handleRatingReminder(ConsumerRecord<String, Map<String, Object>> record,
                                     Acknowledgment acknowledgment) {
        try {
            log.info("Received rating reminder event");
            Map<String, Object> data = record.value();
            
            String riderEmail = NotificationEventFields.text(data, "riderEmail");
            String riderName = NotificationEventFields.text(data, "riderName");
            String driverName = NotificationEventFields.text(data, "driverName");
            
            if (riderEmail != null && riderName != null) {
                notificationService.sendRatingReminderNotification(riderEmail, riderName, driverName);
            }
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing rating reminder notification", e);
            throw new IllegalStateException("Rating notification failed", e);
        }
    }
}
