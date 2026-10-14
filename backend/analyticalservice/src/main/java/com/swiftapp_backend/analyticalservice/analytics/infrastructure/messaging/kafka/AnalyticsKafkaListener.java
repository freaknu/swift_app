package com.swiftapp_backend.analyticalservice.analytics.infrastructure.messaging.kafka;

import com.swiftapp_backend.analyticalservice.analytics.application.service.AnalyticsEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsKafkaListener {
    private final AnalyticsEventService analyticsEventService;

        @KafkaListener(groupId = "analytics-service",
            topics = "${app.kafka.ride-events-topic:ride-events}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String,Object> records, Acknowledgment acknowledgment) {
        String topic = records.topic();
        Object dto = records.value();
        try {
            analyticsEventService.handle(topic, dto);
        }
        catch (Exception e) {
            log.error("Error while consuming , {}",e.getMessage());
            throw e;
        }
    }
}
