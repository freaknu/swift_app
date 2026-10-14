package com.swiftapp_backend.locationservice.location.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.locationservice.location.api.dto.event.LocationUpdateDto;
import com.swiftapp_backend.locationservice.location.application.mapper.LocationUpdateMapper;
import com.swiftapp_backend.locationservice.location.application.port.LocationCachePort;
import com.swiftapp_backend.locationservice.location.application.port.LocationEventPublisher;
import com.swiftapp_backend.locationservice.location.domain.events.LocationEventUpdate;
import com.swiftapp_backend.locationservice.location.domain.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.Instant;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocationServiceMatchingPublishTest {
    private final LocationCachePort cache = mock(LocationCachePort.class);
    private final LocationEventPublisher publisher = mock(LocationEventPublisher.class);
    private final LocationPersistenceService persistence = mock(LocationPersistenceService.class);
    private final DriverPresenceClient presenceClient = mock(DriverPresenceClient.class);
    private final RideLocationAccessClient rideAccessClient = mock(RideLocationAccessClient.class);
    private LocationService service;

    @BeforeEach
    void setUp() {
        service = new LocationService(cache, new ObjectMapper().findAndRegisterModules(),
                mock(LocationUpdateMapper.class), mock(LocationRepository.class),
                persistence, publisher, presenceClient, rideAccessClient);
        when(cache.getList("driverLocation:55")).thenReturn(List.of());
    }

    @Test
    void onlinePositionPublishesProfileDerivedEligibilityAndVehicle() {
        when(presenceClient.get(42L, 55L)).thenReturn(
                new DriverPresenceClient.Presence("ACTIVE", "SEDAN"));
        LocationUpdateDto location = LocationUpdateDto.builder().driverId(55L)
                .latitude(12.97).longitude(77.59).build();

        service.addDriverLocation(location, 42L);

        verify(cache).addGeoLocation("availableDrivers", "55", 12.97, 77.59);
        org.mockito.ArgumentCaptor<LocationEventUpdate> event =
                org.mockito.ArgumentCaptor.forClass(LocationEventUpdate.class);
        verify(publisher).publish(eq("driverlocation.v1"), eq("55"), event.capture());
        assertEquals("ACTIVE", event.getValue().getDriverStatus());
        assertEquals("SEDAN", event.getValue().getVehicleTypeEnum());
    }

    @Test
    void offlinePositionRemovesAvailableDriverAndPublishesInactive() {
        when(presenceClient.get(42L, 55L)).thenReturn(
                new DriverPresenceClient.Presence("INACTIVE", "SEDAN"));
        LocationUpdateDto location = LocationUpdateDto.builder().driverId(55L)
                .latitude(12.97).longitude(77.59).build();

        service.addDriverLocation(location, 42L);

        verify(cache).removeGeoLocation("availableDrivers", "55");
        org.mockito.ArgumentCaptor<LocationEventUpdate> event =
                org.mockito.ArgumentCaptor.forClass(LocationEventUpdate.class);
        verify(publisher).publish(eq("driverlocation.v1"), eq("55"), event.capture());
        assertEquals("INACTIVE", event.getValue().getDriverStatus());
    }

    @Test
    void authenticatedRideFallbackRelaysPositionToRiderTopic() {
        when(presenceClient.get(42L, 55L)).thenReturn(
                new DriverPresenceClient.Presence("INACTIVE", "SEDAN"));
        when(rideAccessClient.assignedToDriver(80L, 55L)).thenReturn(
                new RideLocationAccessClient.Ride(80L, 91L, 55L, "STARTED"));
        when(cache.getList("rideLocation:80")).thenReturn(List.of());
        LocationUpdateDto location = LocationUpdateDto.builder().driverId(55L)
                .rideId(80L).latitude(12.97).longitude(77.59).build();

        service.addDriverLocation(location, 42L);

        assertEquals(91L, location.getRiderId());
        assertNotNull(location.getEventId());
        assertNotNull(location.getEventTime());
        verify(cache).removeGeoLocation("availableDrivers", "55");
        verify(publisher).publishConfirmed(eq("location-updated-topic"), eq("ride-location-80"), eq(location));
    }

    @Test
    void unassignedRideCannotPublishOrPersistLocation() {
        when(presenceClient.get(42L, 55L)).thenReturn(
                new DriverPresenceClient.Presence("ACTIVE", "SEDAN"));
        when(rideAccessClient.assignedToDriver(80L, 55L)).thenThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.addDriverLocation(LocationUpdateDto.builder().driverId(55L)
                        .rideId(80L).latitude(12.97).longitude(77.59).build(), 42L));
        verify(cache, never()).append(eq("rideLocation:80"), any());
        verify(publisher, never()).publishConfirmed(eq("location-updated-topic"), any(), any());
    }

    @Test
    void nearbyDriversRequireFreshOnlinePositionAndExactVehicleType() {
        when(cache.findGeoMembersWithin("availableDrivers", 12.97, 77.59, 5.0))
                .thenReturn(List.of("55", "56", "57"));
        when(cache.getList("driverLocation:55")).thenReturn(List.of(position(55L, "ACTIVE", "SEDAN", Instant.now())));
        when(cache.getList("driverLocation:56")).thenReturn(List.of(position(56L, "ACTIVE", "SEDAN",
                Instant.now().minus(Duration.ofMinutes(2)))));
        when(cache.getList("driverLocation:57")).thenReturn(List.of(position(57L, "INACTIVE", "SEDAN", Instant.now())));

        var result = service.getAllDrivers(12.97, 77.59, 5.0, "SEDAN");

        assertEquals(List.of(55L), result.stream().map(r -> r.getDriverId()).toList());
        assertEquals("SEDAN", result.getFirst().getVehicleType());
        assertNotNull(result.getFirst().getUpdatedAt());
        verify(cache).removeGeoLocation("availableDrivers", "56");
        verify(cache).removeGeoLocation("availableDrivers", "57");
        assertEquals(List.of(), service.getAllDrivers(12.97, 77.59, 5.0, "HATCHBACK"));
    }

    @Test
    void participantGetsLatestRidePositionOnReconnect() {
        when(rideAccessClient.participant(80L, null, 91L)).thenReturn(
                new RideLocationAccessClient.Ride(80L, 91L, 55L, "STARTED"));
        when(cache.getList("rideLocation:80")).thenReturn(List.of(LocationUpdateDto.builder()
                .driverId(55L).rideId(80L).riderId(91L).latitude(12.97)
                .longitude(77.59).eventTime(Instant.now()).build()));

        LocationUpdateDto position = service.getDriverPosition(80L, 91L);

        assertEquals(55L, position.getDriverId());
        assertEquals(12.97, position.getLatitude());
    }

    private LocationUpdateDto position(Long driverId, String status, String type, Instant eventTime) {
        return LocationUpdateDto.builder().driverId(driverId).latitude(12.97).longitude(77.59)
                .driverStatus(status).vehicleType(type).eventTime(eventTime).build();
    }
}
