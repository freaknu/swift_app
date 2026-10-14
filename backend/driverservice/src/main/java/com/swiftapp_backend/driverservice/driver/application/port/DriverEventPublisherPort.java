package com.swiftapp_backend.driverservice.driver.application.port;

public interface DriverEventPublisherPort {
    void publish(String topic, String key, Object event);
}
