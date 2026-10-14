package com.swiftapp_backend.matchingservice.matching.support;

import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverLockRepository;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory fake with real acquire/release (compare-and-delete) semantics. */
public class InMemoryDriverLockRepository implements DriverLockRepository {

    private final Map<Long, Long> locks = new HashMap<>();

    @Override
    public synchronized boolean acquireLock(Long driverId, Long rideId, Duration ttl) {
        if (locks.containsKey(driverId)) {
            return false;
        }
        locks.put(driverId, rideId);
        return true;
    }

    @Override
    public synchronized boolean releaseLock(Long driverId, Long rideId) {
        if (rideId.equals(locks.get(driverId))) {
            locks.remove(driverId);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean isLocked(Long driverId) {
        return locks.containsKey(driverId);
    }

    @Override
    public synchronized Optional<Long> lockedBy(Long driverId) {
        return Optional.ofNullable(locks.get(driverId));
    }

    /** Test helper: simulate another ride holding the lock. */
    public synchronized void forceLock(Long driverId, Long rideId) {
        locks.put(driverId, rideId);
    }
}
