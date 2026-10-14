package com.swiftapp_backend.locationservice.eta.application.port.out;

import com.swiftapp_backend.locationservice.eta.domain.model.RouteCoordinates;

import java.util.Optional;

/** Short-lived route cache; ride/driver identities are never cached with a route. */
public interface EtaCachePort {
    Optional<RouteResult> find(RouteCoordinates coordinates, String rideType);

    void save(RouteCoordinates coordinates, String rideType, RouteResult result);
}
