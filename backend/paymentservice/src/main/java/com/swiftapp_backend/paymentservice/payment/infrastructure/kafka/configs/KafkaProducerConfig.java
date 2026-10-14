package com.swiftapp_backend.paymentservice.payment.infrastructure.kafka.configs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.*;
import java.util.Map;
@Configuration
public class KafkaProducerConfig {
    @Bean public ProducerFactory<String, String> producerFactory(@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String brokers) {
        return new DefaultKafkaProducerFactory<>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.ACKS_CONFIG, "all",
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true));
    }
    @Bean public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producers) { return new KafkaTemplate<>(producers); }
}
