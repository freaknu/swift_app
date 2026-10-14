package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumeConfig {
    @Value("${spring.kafka.listener.auto-startup:true}")
    private boolean listenerAutoStartup = true;
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    public Map<String,Object> consumerProperties() {
        Map<String,Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,100);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,300000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,10000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,3000);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG,1);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 1024 * 1024);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG,30000);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,"chat-service");
        return props;
    }


    @Bean
    public ConsumerFactory<String,Object> consumerFactory() {
        JacksonJsonDeserializer<Object> valueDeserializer = new JacksonJsonDeserializer<>(Object.class);
        valueDeserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(),
                new StringDeserializer(),
                valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String,Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setAutoStartup(listenerAutoStartup);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
