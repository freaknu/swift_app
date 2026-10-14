package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.PendingOfferDto;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.MatchingDecisionPort;
import com.swiftapp_backend.driverservice.driver.application.port.RiderRideLookupPort;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Offer inbox rules: ownership via auth user id, expiry, and the exact
 * Kafka contracts the matching service consumes.
 */
@ExtendWith(MockitoExtension.class)
class DriverOfferServiceTest {

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DriverCachePort cachePort;
    @Mock
    private MatchingDecisionPort matchingClient;
    @Mock
    private RiderRideLookupPort riderRideLookupClient;
    @Mock
    private DriverRegistrationService registrationService;

    private DriverOfferService service;

    @BeforeEach
    void setUp() {
        service = new DriverOfferService(
                driverRepository, vehicleRepository, cachePort, matchingClient,
                riderRideLookupClient,
                registrationService, new ObjectMapper());
    }

    private void givenDriver(long authUserId, long driverId) {
        when(driverRepository.findByAuthUserId(authUserId)).thenReturn(
                Optional.of(DriverModel.builder()
                        .id(driverId)
                        .authUserId(authUserId)
                        .build()));
    }

    private void givenPendingOffer(long driverId, String expiresAt) {
        PendingOfferDto offer = PendingOfferDto.builder()
                .rideId(91004L)
                .riderId(88001L)
                .driverId(driverId)
                .distanceKm(0.5)
                .expiresAt(expiresAt)
                .build();
        // The cache stores JSON; the service converts it back.
        when(cachePort.get("driver:pending-offer:" + driverId))
                .thenReturn(new ObjectMapper().convertValue(offer, Map.class));
    }

    @Test
    void getPendingOffersReturnsStagedOffer() {
        givenDriver(77001L, 102L);
        givenPendingOffer(102L, Instant.now().plusSeconds(30).toString());

        var offers = service.getPendingOffers(77001L);

        assertEquals(1, offers.size());
        assertEquals(91004L, offers.get(0).getRideId());
    }

    @Test
    void getPendingOffersUnknownUserThrowsNotFound() {
        when(driverRepository.findByAuthUserId(99999L))
                .thenReturn(Optional.empty());

        assertThrows(OfferNotFoundException.class,
                () -> service.getPendingOffers(99999L));
    }

    @Test
    void acceptRequiresMatchingConfirmationAndMarksDriverBusy() throws Exception {
        givenDriver(77001L, 102L);
        givenPendingOffer(102L, Instant.now().plusSeconds(30).toString());
        when(vehicleRepository.findAllByDriverId(102L)).thenReturn(List.of(
                VehicleModel.builder().id(81L).isActive(true).build()));
        when(matchingClient.accept(91004L, 102L, 81L)).thenReturn("ACCEPTED");

        service.acceptOffer(77001L, 91004L);

        verify(matchingClient).accept(91004L, 102L, 81L);
        verify(registrationService).changeStatus(77001L, DriverStatus.BUSY);
        verify(cachePort).delete("driver:pending-offer:102");
        verify(cachePort).set("driver:active-ride:102", 91004L);
    }

    @Test
    void acceptExpiredOfferThrowsGone() {
        givenDriver(77001L, 102L);
        givenPendingOffer(102L, Instant.now().minusSeconds(5).toString());

        assertThrows(OfferExpiredException.class,
                () -> service.acceptOffer(77001L, 91004L));
        verify(matchingClient, never()).accept(any(), any(), any());
    }

    @Test
    void acceptWrongRideIdThrowsNotFound() {
        givenDriver(77001L, 102L);
        givenPendingOffer(102L, Instant.now().plusSeconds(30).toString());

        assertThrows(OfferNotFoundException.class,
                () -> service.acceptOffer(77001L, 12345L));
        verify(matchingClient, never()).accept(any(), any(), any());
    }

    @Test
    void cannotAcceptAnotherDriversOffer() {
        // Offer staged under driver 102; a different auth user resolves to
        // driver 103, whose inbox is empty.
        givenDriver(77002L, 103L);
        when(cachePort.get("driver:pending-offer:103")).thenReturn(null);

        assertThrows(OfferNotFoundException.class,
                () -> service.acceptOffer(77002L, 91004L));
        verify(matchingClient, never()).accept(any(), any(), any());
    }

    @Test
    void rejectRequiresMatchingConfirmation() {
        givenDriver(77001L, 102L);
        givenPendingOffer(102L, Instant.now().plusSeconds(30).toString());
        when(matchingClient.reject(91004L, 102L, "too_far")).thenReturn("REJECTED");

        service.rejectOffer(77001L, 91004L, "too_far");

        verify(matchingClient).reject(91004L, 102L, "too_far");
        verify(cachePort).delete("driver:pending-offer:102");
    }

    @Test
    void rejectWithoutReasonDefaultsToDeclined() {
        givenDriver(77001L, 102L);
        givenPendingOffer(102L, Instant.now().plusSeconds(30).toString());
        when(matchingClient.reject(91004L, 102L, "declined")).thenReturn("REJECTED");

        service.rejectOffer(77001L, 91004L, null);

        verify(matchingClient).reject(91004L, 102L, "declined");
    }

    @Test
    void savePendingOfferWritesWithTtl() {
        service.savePendingOffer(102L, 91004L, 88001L, 0.5,
                Instant.now().plusSeconds(15).toString());

        verify(cachePort).set(
                eq("driver:pending-offer:102"), any(PendingOfferDto.class),
                eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void pendingOfferRetainsRequestedCanonicalVehicleType() {
        service.savePendingOffer(102L, 91004L, 88001L, 0.5,
                Instant.now().plusSeconds(15).toString(),
                12.1, 77.1, 12.2, 77.2, 4.5, 600L,
                new java.math.BigDecimal("120.00"), "INR", "HATCHBACK");

        ArgumentCaptor<PendingOfferDto> stored = ArgumentCaptor.forClass(PendingOfferDto.class);
        verify(cachePort).set(eq("driver:pending-offer:102"), stored.capture(),
                eq(60L), eq(TimeUnit.SECONDS));
        assertEquals("HATCHBACK", stored.getValue().getRideTypeCode());
    }

    @Test
    void expiredOfferIsEvictedFromInbox() {
        givenDriver(77001L, 102L);
        givenPendingOffer(102L, Instant.now().minusSeconds(5).toString());

        var offers = service.getPendingOffers(77001L);

        assertTrue(offers.isEmpty());
        verify(cachePort).delete("driver:pending-offer:102");
    }

    @Test
    void activeRideRecoveryUsesRiderDatabaseAndFallsBackToCacheOnOutage() {
        givenDriver(77001L, 102L);
        when(riderRideLookupClient.activeRideId(102L)).thenReturn(91004L)
                .thenThrow(new IllegalStateException("rider service down"));
        when(cachePort.get("driver:active-ride:102")).thenReturn(91004L);

        assertEquals(91004L, service.getActiveRideId(77001L));
        assertEquals(91004L, service.getActiveRideId(77001L));
        verify(cachePort).set("driver:active-ride:102", 91004L);
    }

    @Test
    void completedRideClearsCacheAndRestoresBusyDriverOnline() throws Exception {
        when(cachePort.get("driver:active-ride:102")).thenReturn(91004L);
        when(driverRepository.findById(102L)).thenReturn(Optional.of(
                DriverModel.builder().id(102L).authUserId(77001L)
                        .driverStatus(DriverStatus.BUSY).build()));

        service.clearActiveRide(102L, 91004L);

        verify(cachePort).delete("driver:active-ride:102");
        verify(registrationService).changeStatus(77001L, DriverStatus.ONLINE);
    }
}
