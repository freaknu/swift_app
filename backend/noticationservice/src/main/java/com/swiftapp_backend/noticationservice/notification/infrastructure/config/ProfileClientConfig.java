package com.swiftapp_backend.noticationservice.notification.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class ProfileClientConfig {
    @Bean
    @LoadBalanced
    public RestTemplate profileRestTemplate(RestTemplateBuilder builder) {
        return builder.connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5)).build();
    }
}
