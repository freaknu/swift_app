package com.swiftapp_backend.locationservice.location.infrastructure.messaging.kafka;

import com.swiftapp_backend.locationservice.location.application.port.LocationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLocationEventPublisher implements LocationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, String key, Object event) {

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {

            if (ex != null) {
                log.error("Failed to send message", ex);
                return;
            }

            log.info(
                    "Message sent successfully. Topic={}, Partition={}, Offset={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }

    @Override
    public void publishConfirmed(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Location relay interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Location relay failed", ex);
        }
    }
}
