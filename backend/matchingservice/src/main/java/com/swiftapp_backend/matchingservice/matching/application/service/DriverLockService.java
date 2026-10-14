package com.swiftapp_backend.matchingservice.matching.application.service;

import com.swiftapp_backend.matchingservice.matching.application.config.MatchingProperties;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Application-level API over the distributed driver lock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLockService {

    private final DriverLockRepository driverLockRepository;

    private final MatchingProperties properties;

    public boolean acquire(Long driverId, Long rideId) {
        boolean acquired = driverLockRepository.acquireLock(
                driverId, rideId, Duration.ofSeconds(properties.getLockTtlSeconds()));
        if (!acquired) {
            Optional<Long> holder = driverLockRepository.lockedBy(driverId);
            log.debug("Driver {} lock held by ride {}, skipping for ride {}",
                    driverId, holder.orElse(null), rideId);
        }
        return acquired;
    }

    /**
     * Releases the lock only if this ride still holds it.
     * Safe to call from timeout/reject handlers even if the lock
     * already expired and was re-acquired by another ride.
     */
    public boolean release(Long driverId, Long rideId) {
        return driverLockRepository.releaseLock(driverId, rideId);
    }

    public boolean isLocked(Long driverId) {
        return driverLockRepository.isLocked(driverId);
    }
}
