package com.swiftapp_backend.websocketservice.realtime.application.service;

import com.swiftapp_backend.websocketservice.realtime.application.mapper.LocationUpdateMapper;
import com.swiftapp_backend.websocketservice.realtime.application.port.LocationEventPublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.enums.Direction;
import com.swiftapp_backend.websocketservice.realtime.domain.event.LocationUpdateEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.model.LocationUpdate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LiveLocationServiceTest {

    @Test
    void publishesMappedLocationThroughPort() {
        LocationEventPublisher publisher = mock(LocationEventPublisher.class);
        LiveLocationService service = new LiveLocationService(publisher, new LocationUpdateMapper());
        LocationUpdate location = LocationUpdate.builder()
                .id(1L)
                .driverId(11L)
                .rideId(101L)
                .riderId(22L)
                .latitude(28.6139)
                .longitude(77.2090)
                .nearByPlace("Delhi")
                .direction(Direction.NORTH)
                .build();

        service.consumeLocation(location);

        ArgumentCaptor<LocationUpdateEvent> eventCaptor =
                ArgumentCaptor.forClass(LocationUpdateEvent.class);
        verify(publisher).publishLocationUpdate(eventCaptor.capture());
        LocationUpdateEvent event = eventCaptor.getValue();
        assertThat(event.getId()).isEqualTo(location.getId());
        assertThat(event.getDriverId()).isEqualTo(location.getDriverId());
        assertThat(event.getRideId()).isEqualTo(location.getRideId());
        assertThat(event.getRiderId()).isEqualTo(location.getRiderId());
        assertThat(event.getLatitude()).isEqualTo(location.getLatitude());
        assertThat(event.getLongitude()).isEqualTo(location.getLongitude());
        assertThat(event.getNearByPlace()).isEqualTo(location.getNearByPlace());
        assertThat(event.getDirection()).isEqualTo(location.getDirection());
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getEventTime()).isNotNull();
    }
}
