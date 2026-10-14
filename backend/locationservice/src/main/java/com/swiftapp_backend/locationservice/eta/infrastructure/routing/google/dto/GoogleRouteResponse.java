package com.swiftapp_backend.locationservice.eta.infrastructure.routing.google.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleRouteResponse {
    private List<Route> routes;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Route {
        private Long distanceMeters;
        private String duration;
        private String staticDuration;
        private Polyline polyline;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Polyline {
        private String encodedPolyline;
    }
}
