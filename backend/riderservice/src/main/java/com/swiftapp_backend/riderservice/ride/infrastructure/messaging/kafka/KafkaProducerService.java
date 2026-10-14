package com.swiftapp_backend.riderservice.ride.infrastructure.messaging.kafka;

import com.swiftapp_backend.riderservice.ride.application.port.MessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService implements MessagePublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RideCompletionOutbox completionOutbox;

    @Override
    public void publish(String topic, String key, Object event) {
        if ("ride.completed".equals(topic) || "ride-completed".equals(topic)) {
            completionOutbox.enqueue(topic, key, event);
            return;
        }

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
}
