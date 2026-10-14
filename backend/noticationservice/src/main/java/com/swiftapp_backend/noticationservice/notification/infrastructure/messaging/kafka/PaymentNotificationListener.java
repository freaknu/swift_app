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
public class PaymentNotificationListener {
    private final NotificationService notificationService;

    @KafkaListener(
            topics = {"payment.completed", "payment-completed"},
            groupId = "notification-service"
    )
    public void handlePaymentCompleted(ConsumerRecord<String, Map<String, Object>> record,
                                       Acknowledgment acknowledgment) {
        try {
            log.info("Received payment completed event");
            Map<String, Object> data = record.value();
            
            String riderEmail = NotificationEventFields.text(data, "riderEmail");
            String riderName = NotificationEventFields.text(data, "riderName");
            Double amount = NotificationEventFields.amount(data, "amount", null);
            String paymentMethod = NotificationEventFields.text(data, "paymentMethod");
            
            if (riderEmail != null && riderName != null) {
                notificationService.sendPaymentSuccessNotification(riderEmail, riderName, amount, paymentMethod);
            }
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment completed notification", e);
            throw new IllegalStateException("Payment notification failed", e);
        }
    }

    @KafkaListener(
            topics = {"payment.failed", "payment-failed"},
            groupId = "notification-service"
    )
    public void handlePaymentFailed(ConsumerRecord<String, Map<String, Object>> record,
                                    Acknowledgment acknowledgment) {
        try {
            log.info("Received payment failed event");
            Map<String, Object> data = record.value();
            
            String riderEmail = NotificationEventFields.text(data, "riderEmail");
            String riderName = NotificationEventFields.text(data, "riderName");
            Double amount = NotificationEventFields.amount(data, "amount", null);
            String reason = NotificationEventFields.text(data, "reason");
            
            if (riderEmail != null && riderName != null) {
                notificationService.sendPaymentFailedNotification(riderEmail, riderName, amount, reason);
            }
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment failed notification", e);
            throw new IllegalStateException("Payment notification failed", e);
        }
    }
}
