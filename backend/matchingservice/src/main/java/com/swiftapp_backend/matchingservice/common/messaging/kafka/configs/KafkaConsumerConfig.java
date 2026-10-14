package com.swiftapp_backend.matchingservice.common.messaging.kafka.configs;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.listener.auto-startup:true}")
    private boolean listenerAutoStartup = true;
    private final List<String> bootstrapServers;

    public KafkaConsumerConfig(Environment environment) {
        bootstrapServers = Binder.get(environment)
                .bind("spring.kafka.bootstrap-servers", Bindable.listOf(String.class))
                .orElse(List.of("localhost:9092"));
    }

    public Map<String, Object> properties() {

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        // Receive Kafka JSON as String
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "matching-service"
        );

        props.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                40
        );

        props.put(
                ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                30000
        );

        props.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        props.put(
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
                10000
        );

        props.put(
                ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG,
                540000
        );

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "latest"
        );

        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(properties());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setAutoStartup(listenerAutoStartup);

        // IMPORTANT
        // Required because KafkaConsumer uses Acknowledgment
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL
        );

        return factory;
    }
}
