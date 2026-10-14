package com.swiftapp_backend.riderservice.ride.application.port;

/**
 * Outbound message bus boundary used by ride use cases.
 */
public interface MessagePublisherPort {
    void publish(String topic, String key, Object event);
}
