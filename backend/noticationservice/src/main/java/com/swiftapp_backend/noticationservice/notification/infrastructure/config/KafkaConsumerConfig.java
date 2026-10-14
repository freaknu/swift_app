package com.swiftapp_backend.noticationservice.notification.infrastructure.config;

import com.swiftapp_backend.noticationservice.notification.api.dto.OtpSendNotificationDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.listener.auto-startup:true}")
    private boolean listenerAutoStartup = true;
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Map<String, Object>> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProperties("java.util.HashMap"));
    }

    @Bean
    public ConsumerFactory<String, OtpSendNotificationDto> otpConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProperties(
                OtpSendNotificationDto.class.getName()));
    }

    private Map<String, Object> consumerProperties(String valueType) {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);

        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);

        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
                StringDeserializer.class);

        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class);

        props.put(JsonDeserializer.TRUSTED_PACKAGES,
                "java.util,com.swiftapp_backend.noticationservice.notification.api.dto");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType);
        return props;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>>
         kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setAutoStartup(listenerAutoStartup);

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OtpSendNotificationDto>
         otpKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OtpSendNotificationDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(otpConsumerFactory());
        factory.setAutoStartup(listenerAutoStartup);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
