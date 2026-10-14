package com.swiftapp_backend.paymentservice.payment.infrastructure.kafka.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.paymentservice.payment.application.service.PaymentService;
import com.swiftapp_backend.paymentservice.payment.domain.event.RideCompletedEvent;
import com.swiftapp_backend.paymentservice.payment.infrastructure.kafka.topics.KafkaTopics;
import com.swiftapp_backend.paymentservice.shared.SafeFailureDetails;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class KafkaConsumerService {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final ObjectMapper mapper;
    private final PaymentService payments;
    @KafkaListener(groupId = "payment-service", topics = KafkaTopics.RIDE_COMPLETED,
                   autoStartup = "${payment.consumer.enabled:true}")
    public void consume(ConsumerRecord<String, String> record) throws java.io.IOException {
        try {
            consume(record.value());
        } catch (java.io.IOException | RuntimeException ex) {
            log.error("Ride payment processing failed topic={} partition={} offset={} failure={}",
                record.topic(), record.partition(), record.offset(), SafeFailureDetails.describe(ex));
            throw ex;
        }
    }

    public void consume(String payload) throws java.io.IOException {
        payments.handleRideComplete(mapper.readValue(payload, RideCompletedEvent.class));
    }
}
