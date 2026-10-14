package com.swiftapp_backend.matchingservice.matching.application.strategy;

import java.util.List;

/**
 * Distributed round-robin cursor. Backed by Redis INCR so it stays
 * correct across multiple matching-service instances
 * (never a JVM-local AtomicInteger).
 */
public interface RoundRobinIndex {

    long nextIndex();
}
