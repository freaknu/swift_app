package com.swiftapp_backend.locationservice.eta.infrastructure.routing.google.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleRouteRequest {
    private Location origin;
    private Location destination;
    private String travelMode;
    private String routingPreference;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private LocationWrapper location;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationWrapper {
        private LatLng latLng;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatLng {
        private double latitude;
        private double longitude;
    }
}
