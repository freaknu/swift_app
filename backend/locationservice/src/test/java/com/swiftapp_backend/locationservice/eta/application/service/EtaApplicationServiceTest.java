package com.swiftapp_backend.locationservice.eta.application.service;

import com.swiftapp_backend.locationservice.eta.application.port.out.EtaCachePort;
import com.swiftapp_backend.locationservice.eta.application.port.out.EtaEventPublisher;
import com.swiftapp_backend.locationservice.eta.application.port.out.RouteResult;
import com.swiftapp_backend.locationservice.eta.application.port.out.RoutingPort;
import com.swiftapp_backend.locationservice.eta.domain.model.Eta;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EtaApplicationServiceTest {
    private final RoutingPort routingPort = mock(RoutingPort.class);
    private final EtaCachePort etaCache = mock(EtaCachePort.class);
    private final EtaEventPublisher eventPublisher = mock(EtaEventPublisher.class);
    private final EtaApplicationService service =
            new EtaApplicationService(routingPort, etaCache, eventPublisher);

    @Test
    void usesTrafficDurationAndPublishesTheProviderNeutralEta() {
        when(etaCache.find(any(), any())).thenReturn(Optional.empty());
        when(routingPort.calculateRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(new RouteResult(5800L, 870L, 700L, "enc-polyline"));

        Eta eta = service.calculateEta("ride-1", "driver-2", 28.6139, 77.2090,
                28.6280, 77.2190, "STANDARD");

        assertThat(eta.rideId()).isEqualTo("ride-1");
        assertThat(eta.driverId()).isEqualTo("driver-2");
        assertThat(eta.distanceKm()).isEqualTo(5.8);
        assertThat(eta.durationSeconds()).isEqualTo(870L);
        assertThat(eta.etaMinutes()).isEqualTo(15L);
        assertThat(eta.encodedPolyline()).isEqualTo("enc-polyline");
        verify(etaCache).save(any(), eq("STANDARD"), eq(new RouteResult(5800L, 870L, 700L, "enc-polyline")));
        verify(eventPublisher).publish(eta);
    }

    @Test
    void reusesCachedRouteWithoutCallingProvider() {
        RouteResult cached = new RouteResult(1200L, 61L, 59L, null);
        when(etaCache.find(any(), any())).thenReturn(Optional.of(cached));

        Eta eta = service.calculateEta("ride-1", "driver-2", 28.6139, 77.2090,
                28.6280, 77.2190, "BIKE");

        assertThat(eta.etaMinutes()).isEqualTo(2L);
        assertThat(eta.encodedPolyline()).isNull();
        verifyNoInteractions(routingPort);
        verify(etaCache, never()).save(any(), any(), any());
    }

    @Test
    void forwardsRideTypeToRoutingPortAndCache() {
        when(etaCache.find(any(), any())).thenReturn(Optional.empty());
        when(routingPort.calculateRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(new RouteResult(5800L, 870L, 700L, "enc-polyline"));

        service.calculateEta("ride-1", "driver-2", 28.6139, 77.2090,
                28.6280, 77.2190, "BIKE");

        verify(routingPort).calculateRoute(eq(28.6139), eq(77.2090), eq(28.6280), eq(77.2190), eq("BIKE"));
        verify(etaCache).save(any(), eq("BIKE"), eq(new RouteResult(5800L, 870L, 700L, "enc-polyline")));
    }
}
