package com.swiftapp_backend.matchingservice.matching.domain.repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Distributed lock preventing two rides from offering to the same driver.
 * Implementations must be atomic (Redis SET NX EX).
 */
public interface DriverLockRepository {

    /**
     * @return true if the lock was acquired, false if another ride holds it.
     */
    boolean acquireLock(Long driverId, Long rideId, Duration ttl);

    /**
     * Releases the lock only if it is still held by the given ride.
     * Must never delete another ride's lock (atomic compare-and-delete).
     *
     * @return true if a lock was released.
     */
    boolean releaseLock(Long driverId, Long rideId);

    boolean isLocked(Long driverId);

    Optional<Long> lockedBy(Long driverId);
}
