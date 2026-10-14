package com.swiftapp_backend.matchingservice.matching.infrastructure.messaging.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka producer for matching events (driver.matched, ride.offer.broadcast,
 * ride.match.failed). Values are serialized as JSON.
 */
@Configuration
public class KafkaProducerConfig {

    private final List<String> bootstrapServers;

    public KafkaProducerConfig(Environment environment) {
        bootstrapServers = Binder.get(environment)
                .bind("spring.kafka.bootstrap-servers", Bindable.listOf(String.class))
                .orElse(List.of("localhost:9092"));
    }

    @Bean
    public ProducerFactory<String, Object> matchingProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Don't add type-info headers; consumers here read plain JSON.
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // Fail fast when the broker is unreachable. driver.matched and
        // ride.match.failed have Redis outboxes retried by the dispatcher.
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> matchingKafkaTemplate(
            ProducerFactory<String, Object> matchingProducerFactory) {
        return new KafkaTemplate<>(matchingProducerFactory);
    }
}
