package com.swiftapp_backend.userservice.user.infrastructure.messaging.kafka;

import com.swiftapp_backend.userservice.user.application.port.UserEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaUserProducerService implements UserEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, String key, Object event) {
        log.info("Publishing user event to topic={}, key={}", topic, key);
        kafkaTemplate.send(topic, key, event);
    }
}
