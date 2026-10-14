package com.swiftapp_backend.locationservice.eta.application.port.in;

import com.swiftapp_backend.locationservice.eta.domain.model.Eta;

public interface CalculateEtaUseCase {
    /**
     * @param rideType optional ride type (e.g. BIKE, AUTO, STANDARD); selects the
     *                 routing travel mode, defaulting to DRIVE when unknown or blank
     */
    Eta calculateEta(
            String rideId,
            String driverId,
            double driverLatitude,
            double driverLongitude,
            double destinationLatitude,
            double destinationLongitude,
            String rideType
    );
}
