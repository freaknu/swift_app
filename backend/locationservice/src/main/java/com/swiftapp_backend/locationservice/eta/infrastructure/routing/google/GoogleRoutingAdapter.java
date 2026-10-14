package com.swiftapp_backend.locationservice.eta.infrastructure.routing.google;

import com.swiftapp_backend.locationservice.eta.application.port.out.RouteResult;
import com.swiftapp_backend.locationservice.eta.application.port.out.RoutingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleRoutingAdapter implements RoutingPort {
    private final GoogleRoutesClient googleRoutesClient;
    private final GoogleRoutesMapper mapper;

    @Override
    public RouteResult calculateRoute(double originLatitude, double originLongitude,
                                      double destinationLatitude, double destinationLongitude,
                                      String rideType) {
        var request = mapper.toRequest(originLatitude, originLongitude,
                destinationLatitude, destinationLongitude,
                GoogleRoutesMapper.toTravelMode(rideType));
        return mapper.toResult(googleRoutesClient.calculateRoute(request));
    }
}
