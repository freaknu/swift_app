package com.swiftapp_backend.authservice.authentication.application.port;

import java.time.Duration;
import java.util.Optional;

public interface OtpStore {
    void store(String key, int otp, Duration ttl);

    Optional<Integer> find(String key);

    void delete(String key);
}
