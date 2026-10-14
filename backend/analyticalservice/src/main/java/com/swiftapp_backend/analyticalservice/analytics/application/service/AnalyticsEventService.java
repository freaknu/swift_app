package com.swiftapp_backend.analyticalservice.analytics.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnalyticsEventService {
    public void handle(String topic, Object payload) {
        // Topic-specific metric updates belong here as analytics use cases are added.
        log.debug("Received analytics event from topic {}", topic);
    }
}
