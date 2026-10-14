package com.swiftapp_backend.authservice.authentication.application.port;

public interface EventPublisher {
    void publish(String topic, String key, Object event);
}
