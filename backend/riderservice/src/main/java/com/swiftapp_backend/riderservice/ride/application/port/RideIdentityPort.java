package com.swiftapp_backend.riderservice.ride.application.port;

/** Resolves verified auth identities to their rider and driver profile IDs. */
public interface RideIdentityPort {
    Long riderIdForAuthUser(Long authUserId);

    Long driverIdForAuthUser(Long authUserId);

    Long riderAuthUserId(Long riderId);
    Long driverAuthUserId(Long driverId);
}
