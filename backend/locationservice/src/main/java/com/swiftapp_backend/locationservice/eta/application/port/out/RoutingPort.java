package com.swiftapp_backend.locationservice.eta.application.port.out;

public interface RoutingPort {
    /**
     * @param rideType optional ride type (e.g. BIKE, AUTO, STANDARD); the routing
     *                 adapter maps it to the provider travel mode
     */
    RouteResult calculateRoute(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude,
            String rideType
    );
}
