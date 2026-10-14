package com.swiftapp_backend.locationservice.eta.application.service;

import com.swiftapp_backend.locationservice.eta.application.port.in.CalculateEtaUseCase;
import com.swiftapp_backend.locationservice.eta.application.port.out.EtaCachePort;
import com.swiftapp_backend.locationservice.eta.application.port.out.EtaEventPublisher;
import com.swiftapp_backend.locationservice.eta.application.port.out.RouteResult;
import com.swiftapp_backend.locationservice.eta.application.port.out.RoutingPort;
import com.swiftapp_backend.locationservice.eta.domain.model.Eta;
import com.swiftapp_backend.locationservice.eta.domain.model.RouteCoordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EtaApplicationService implements CalculateEtaUseCase {
    private final RoutingPort routingPort;
    private final EtaCachePort etaCache;
    private final EtaEventPublisher eventPublisher;

    @Override
    public Eta calculateEta(String rideId, String driverId, double driverLatitude,
                            double driverLongitude, double destinationLatitude,
                            double destinationLongitude, String rideType) {
        if (rideId == null || rideId.isBlank() || driverId == null || driverId.isBlank()) {
            throw new IllegalArgumentException("rideId and driverId are required");
        }
        RouteCoordinates coordinates = new RouteCoordinates(driverLatitude, driverLongitude,
                destinationLatitude, destinationLongitude);
        RouteResult route = etaCache.find(coordinates, rideType).orElseGet(() -> {
            RouteResult calculated = routingPort.calculateRoute(driverLatitude, driverLongitude,
                    destinationLatitude, destinationLongitude, rideType);
            etaCache.save(coordinates, rideType, calculated);
            return calculated;
        });

        // Use TRAFFIC_AWARE duration, not staticDuration. Round up, including
        // sub-minute trips, without overflowing for a large duration.
        long seconds = route.durationSeconds();
        long minutes = seconds / 60 + (seconds % 60 == 0 ? 0 : 1);
        Eta eta = new Eta(rideId.trim(), driverId.trim(), route.distanceMeters() / 1000.0,
                seconds, minutes, route.encodedPolyline());
        eventPublisher.publish(eta);
        return eta;
    }
}
