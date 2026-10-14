package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.api.dto.OtpSendNotificationDto;
import com.swiftapp_backend.noticationservice.notification.application.mapper.NotificationMapper;
import com.swiftapp_backend.noticationservice.notification.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OtpNotificationListener {
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "otp-notification",
            groupId = "notification-service",
            containerFactory = "otpKafkaListenerContainerFactory"
    )
    public void consume(OtpSendNotificationDto dto,
                        Acknowledgment acknowledgment) {

        try {

            log.info("Received OTP Notification");
            notificationService.sendOtpEmail(NotificationMapper.toDomain(dto));

            acknowledgment.acknowledge();

        } catch (Exception e) {

            log.error("Error while consuming message", e);
            throw new IllegalStateException("Notification service Kafka listener failed", e);
        }
    }
}
