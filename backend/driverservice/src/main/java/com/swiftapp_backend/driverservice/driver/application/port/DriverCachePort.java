package com.swiftapp_backend.driverservice.driver.application.port;

import java.util.concurrent.TimeUnit;

/**
 * Application-facing cache boundary. Redis is only one possible implementation.
 */
public interface DriverCachePort {

    void set(String key, Object value);

    void set(String key, Object value, long ttl, TimeUnit unit);

    Object get(String key);

    boolean exists(String key);

    void delete(String key);
}
