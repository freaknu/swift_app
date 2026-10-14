package com.swiftapp_backend.matchingservice.matching.application.config;

import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStrategyType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tunables for the matching engine. All have safe defaults so the
 * service starts even if the yaml section is missing.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "matching")
public class MatchingProperties {

    /** Seconds a driver has to accept/reject an offer. */
    private int offerTtlSeconds = 15;

    /** TTL of the driver matching lock. */
    private int lockTtlSeconds = 20;

    /** How many offer attempts before the ride fails. */
    private int maxAttempts = 4;

    /** Search radius per attempt (attempt 1 -> radii[0], ...). */
    private List<Double> radiiKm = List.of(5.0, 7.0, 10.0, 15.0);

    /** Locations older than this are treated as stale and skipped. */
    private long staleLocationThresholdSeconds = 60;

    /** How often the timeout worker scans for expired offers. */
    private long timeoutScanIntervalMs = 2000;

    /** Max candidates pulled from GEO per attempt. */
    private long candidateLimit = 25;

    private MatchingStrategyType strategy = MatchingStrategyType.NEAREST;

    public double radiusForAttempt(int attempt) {
        if (radiiKm == null || radiiKm.isEmpty()) {
            return 5.0;
        }
        int index = Math.min(Math.max(attempt - 1, 0), radiiKm.size() - 1);
        return radiiKm.get(index);
    }
}
