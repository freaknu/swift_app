package com.swiftapp_backend.locationservice.location.application.port;

public interface LocationEventPublisher {
    void publish(String topic, String key, Object event);

    default void publishConfirmed(String topic, String key, Object event) {
        publish(topic, key, event);
    }
}
