package com.swiftapp_backend.userservice.user.application.port;

public interface UserEventPublisherPort {
    void publish(String topic, String key, Object event);
}
