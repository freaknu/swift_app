package com.swiftapp_backend.driverservice.driver.application.port;

/** Confirms a driver's offer decision with the matching authority. */
public interface MatchingDecisionPort {
    String accept(Long rideId, Long driverId, Long vehicleId);
    String reject(Long rideId, Long driverId, String reason);
}
