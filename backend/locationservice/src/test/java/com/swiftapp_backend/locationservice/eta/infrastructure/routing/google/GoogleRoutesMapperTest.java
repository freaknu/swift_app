package com.swiftapp_backend.locationservice.eta.infrastructure.routing.google;

import com.swiftapp_backend.locationservice.eta.application.port.out.RoutingException;
import com.swiftapp_backend.locationservice.eta.application.port.out.RouteResult;
import com.swiftapp_backend.locationservice.eta.infrastructure.routing.google.dto.GoogleRouteRequest;
import com.swiftapp_backend.locationservice.eta.infrastructure.routing.google.dto.GoogleRouteResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleRoutesMapperTest {
    private final GoogleRoutesMapper mapper = new GoogleRoutesMapper();

    @Test
    void buildsGoogleRoutesRequestWithTrafficAwareDriving() {
        GoogleRouteRequest request = mapper.toRequest(28.6139, 77.2090, 28.6280, 77.2190, "DRIVE");

        assertThat(request.getTravelMode()).isEqualTo("DRIVE");
        assertThat(request.getRoutingPreference()).isEqualTo("TRAFFIC_AWARE");
        assertThat(request.getOrigin().getLocation().getLatLng().getLatitude()).isEqualTo(28.6139);
        assertThat(request.getOrigin().getLocation().getLatLng().getLongitude()).isEqualTo(77.2090);
        assertThat(request.getDestination().getLocation().getLatLng().getLatitude()).isEqualTo(28.6280);
        assertThat(request.getDestination().getLocation().getLatLng().getLongitude()).isEqualTo(77.2190);
    }

    @Test
    void mapsGoogleResponseAndRoundsProviderFractionsUp() {
        GoogleRouteResponse response = new GoogleRouteResponse(List.of(
                new GoogleRouteResponse.Route(5800L, "870.1s", "700.01s",
                        new GoogleRouteResponse.Polyline("enc-polyline"))
        ));

        RouteResult result = mapper.toResult(response);

        assertThat(result).isEqualTo(new RouteResult(5800L, 871L, 701L, "enc-polyline"));
    }

    @Test
    void toleratesMissingPolyline() {
        GoogleRouteResponse response = new GoogleRouteResponse(List.of(
                new GoogleRouteResponse.Route(5800L, "870s", "700s", null)
        ));

        RouteResult result = mapper.toResult(response);

        assertThat(result.distanceMeters()).isEqualTo(5800L);
        assertThat(result.encodedPolyline()).isNull();
    }

    @Test
    void rejectsEmptyRoutes() {
        assertThatThrownBy(() -> mapper.toResult(new GoogleRouteResponse(List.of())))
                .isInstanceOf(RoutingException.class)
                .hasMessageContaining("No driving route");
    }

    @Test
    void rejectsMalformedDurations() {
        GoogleRouteResponse response = new GoogleRouteResponse(List.of(
                new GoogleRouteResponse.Route(5800L, "870 seconds", "700s", null)
        ));

        assertThatThrownBy(() -> mapper.toResult(response))
                .isInstanceOf(RoutingException.class)
                .hasMessageContaining("invalid route response");
    }

    @Test
    void mapsRideTypesToGoogleTravelModes() {
        assertThat(GoogleRoutesMapper.toTravelMode("BIKE")).isEqualTo("TWO_WHEELER");
        assertThat(GoogleRoutesMapper.toTravelMode("scooter")).isEqualTo("TWO_WHEELER");
        assertThat(GoogleRoutesMapper.toTravelMode("ELECTRIC_BIKE")).isEqualTo("TWO_WHEELER");
        assertThat(GoogleRoutesMapper.toTravelMode("DELIVERY_BIKE")).isEqualTo("TWO_WHEELER");
        assertThat(GoogleRoutesMapper.toTravelMode("HATCHBACK")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("DELIVERY_VAN")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("AUTO")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("STANDARD")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("PREMIUM")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("SUV")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("POOL")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("AIRPORT")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("OUTSTATION")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("RENTAL")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("INTERCITY")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("DELIVERY")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("LUXURY")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("ELECTRIC")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode(null)).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("")).isEqualTo("DRIVE");
        assertThat(GoogleRoutesMapper.toTravelMode("SPACESHIP")).isEqualTo("DRIVE");
    }

    @Test
    void buildsTwoWheelerRequestForBikeRideType() {
        GoogleRouteRequest request = mapper.toRequest(28.6139, 77.2090, 28.6280, 77.2190,
                GoogleRoutesMapper.toTravelMode("BIKE"));

        assertThat(request.getTravelMode()).isEqualTo("TWO_WHEELER");
        assertThat(request.getRoutingPreference()).isEqualTo("TRAFFIC_AWARE");
    }
}
