package com.swiftapp_backend.analyticalservice.analytics.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaBootstrapConfigTest {
    @Test
    void scalarBootstrapOverrideAppliesToAnalyticsConsumer() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.kafka.bootstrap-servers", "127.0.0.1:19092");

        assertEquals(List.of("127.0.0.1:19092"),
                new KafkaConsumerConfig(environment).consumerFactory()
                        .getConfigurationProperties().get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void yamlStyleListBindsToAnalyticsConsumer() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.kafka.bootstrap-servers[0]", "broker-a:9092")
                .withProperty("spring.kafka.bootstrap-servers[1]", "broker-b:9092");

        assertEquals(List.of("broker-a:9092", "broker-b:9092"),
                new KafkaConsumerConfig(environment).consumerFactory()
                        .getConfigurationProperties().get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }
}
