package com.swiftapp_backend.driverservice.driver.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaBootstrapConfigTest {
    @Test
    void scalarBootstrapOverrideAppliesToEveryDriverKafkaFactory() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.kafka.bootstrap-servers", "127.0.0.1:19092");
        KafkaConsumerConfig consumers = new KafkaConsumerConfig(environment);
        KafkaProducerConfig producers = new KafkaProducerConfig(environment);

        assertEquals(List.of("127.0.0.1:19092"),
                consumers.consumerFactory().getConfigurationProperties()
                        .get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("java.util.HashMap", consumers.consumerFactory()
                .getConfigurationProperties().get(JsonDeserializer.VALUE_DEFAULT_TYPE));
        @SuppressWarnings("unchecked")
        DefaultKafkaConsumerFactory<String, String> plain =
                (DefaultKafkaConsumerFactory<String, String>) consumers
                        .plainJsonKafkaListenerContainerFactory().getConsumerFactory();
        assertEquals(List.of("127.0.0.1:19092"),
                plain.getConfigurationProperties().get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        @SuppressWarnings("unchecked")
        DefaultKafkaProducerFactory<String, Object> producer =
                (DefaultKafkaProducerFactory<String, Object>) producers.producerFactory();
        assertEquals(List.of("127.0.0.1:19092"),
                producer.getConfigurationProperties().get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void yamlStyleListBindsToConsumerBootstrapServers() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.kafka.bootstrap-servers[0]", "broker-a:9092")
                .withProperty("spring.kafka.bootstrap-servers[1]", "broker-b:9092");

        assertEquals(List.of("broker-a:9092", "broker-b:9092"),
                new KafkaConsumerConfig(environment).consumerFactory()
                        .getConfigurationProperties().get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }
}
