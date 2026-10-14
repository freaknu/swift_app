package com.swiftapp_backend.userservice.user.application.port;

import java.time.Duration;
import java.util.Optional;

public interface UserCache {
    <T> Optional<T> find(String key, Class<T> valueType);

    void put(String key, Object value, Duration ttl);

    boolean contains(String key);

    void evict(String key);
}
