package com.swiftapp_backend.locationservice.eta.infrastructure.messaging.kafka;

import com.swiftapp_backend.locationservice.eta.domain.model.Eta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EtaEventPublisher implements com.swiftapp_backend.locationservice.eta.application.port.out.EtaEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${eta.kafka-topic:eta.updated}")
    private String topic;

    @Override
    public void publish(Eta eta) {
        try {
            kafkaTemplate.send(topic, eta.rideId(), eta)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Unable to publish ETA update for ride {}", eta.rideId(), error);
                        }
                    });
        } catch (RuntimeException ex) {
            // ETA calculation remains available when Kafka is temporarily down.
            log.warn("Unable to publish ETA update for ride {}", eta.rideId(), ex);
        }
    }
}
