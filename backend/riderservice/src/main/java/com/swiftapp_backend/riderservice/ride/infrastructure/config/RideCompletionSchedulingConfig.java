package com.swiftapp_backend.riderservice.ride.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "ride.completion-outbox.enabled", havingValue = "true", matchIfMissing = true)
public class RideCompletionSchedulingConfig {}
