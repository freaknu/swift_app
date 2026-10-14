package com.swiftapp_backend.driverservice.driver.application.port;

/** Finds the authoritative active ride after a socket or app reconnect. */
public interface RiderRideLookupPort {
    Long activeRideId(Long driverId);
}
