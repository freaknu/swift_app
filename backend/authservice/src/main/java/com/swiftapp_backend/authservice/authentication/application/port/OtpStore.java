package com.swiftapp_backend.authservice.authentication.application.port;

import java.time.Duration;
import java.util.Optional;

public interface OtpStore {
    void store(String key, int otp, Duration ttl);

    Optional<Integer> find(String key);

    /** Validates without deleting or extending the OTP's original expiry. */
    boolean verify(String key, int otp);

    /** Claims a verified OTP for one login session while retaining the code until home is ready. */
    boolean claimVerified(String key, Integer otp, String completionToken, String sessionHash);

    /** Deletes only a claim belonging to the acknowledged session; safe after expiry or a resend. */
    void complete(String key, String completionToken, String sessionHash);

    /** Releases a failed login transaction's claim while preserving the code and its expiry. */
    void release(String key, String completionToken, String sessionHash);

    /** Consumes a valid code for non-login operations, without deleting a newer or claimed code. */
    boolean consume(String key, int otp);

    void delete(String key);
}
