package com.swiftapp_backend.driverservice.driver.infrastructure.messaging.kafka;

import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaDriverEventPublisher implements DriverEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, String key, Object event) {
        log.info("Publishing driver event to topic={}, key={}", topic, key);
        kafkaTemplate.send(topic, key, event);
    }
}
