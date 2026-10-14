package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentChangeVerificationStatus;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverNotificationSettingsPort;
import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverDocumentRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverEarningRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverEarningMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverReviewNotificationProducerTest {
    private static final long AUTH_USER_ID = 410L;
    private static final long DRIVER_ID = 72L;

    @Test
    void documentReviewPublishesProfileTargetedEventOnlyWhenStatusChanges() throws Exception {
        DriverDocumentRepository documents = mock(DriverDocumentRepository.class);
        DriverRepository drivers = mock(DriverRepository.class);
        DriverEventPublisherPort events = mock(DriverEventPublisherPort.class);
        DriverDocumentModel document = DriverDocumentModel.builder()
                .id(31L).authUserId(AUTH_USER_ID).isActive(true)
                .documentType(DocumentType.DRIVING_LICENCE)
                .verificationStatus(VerificationStatus.PENDING)
                .allStatus(new LinkedHashSet<>(List.of(VerificationStatus.PENDING))).build();
        when(documents.findByAuthUserIdAndId(AUTH_USER_ID, 31L)).thenReturn(Optional.of(document));
        when(drivers.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(driver(true)));
        DriverDocumentService service = new DriverDocumentService(documents, drivers,
                mock(DriverCachePort.class), events);

        DriverDocumentChangeVerificationStatus rejected = new DriverDocumentChangeVerificationStatus(
                DRIVER_ID, 900L, 31L, VerificationStatus.REJECTED, "  Image is blurry  ");
        service.changeDocumentVerificationStatus(AUTH_USER_ID, rejected);
        service.changeDocumentVerificationStatus(AUTH_USER_ID, rejected);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(events).publish(eq("driver.document.reviewed"), eq("driver" + AUTH_USER_ID), payload.capture());
        Map<?, ?> event = event(payload.getValue());
        assertEnvelope(event, "REJECTED");
        assertEquals(31L, event.get("documentId"));
        assertEquals("DRIVING_LICENCE", event.get("documentType"));
        assertEquals("Image is blurry", event.get("reason"));

        service.changeDocumentVerificationStatus(AUTH_USER_ID,
                new DriverDocumentChangeVerificationStatus(DRIVER_ID, 900L, 31L,
                        VerificationStatus.APPROVED, null));
        verify(events, times(2)).publish(eq("driver.document.reviewed"),
                eq("driver" + AUTH_USER_ID), payload.capture());
        assertEquals("No review details provided.", event(payload.getAllValues().getLast()).get("reason"));
    }

    @Test
    void vehicleVerificationPublishesOnceForUnverifiedVehicle() throws Exception {
        VehicleRepository vehicles = mock(VehicleRepository.class);
        DriverRepository drivers = mock(DriverRepository.class);
        DriverEventPublisherPort events = mock(DriverEventPublisherPort.class);
        VehicleModel vehicle = VehicleModel.builder()
                .id(52L).authUserId(AUTH_USER_ID).isVerified(false).build();
        when(vehicles.findByAuthUserIdAndId(AUTH_USER_ID, 52L)).thenReturn(Optional.of(vehicle));
        when(drivers.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(driver(true)));
        VehicleService service = new VehicleService(vehicles, drivers,
                mock(DriverCachePort.class), new ObjectMapper(), events);

        service.verifyVehicle(AUTH_USER_ID, 52L);
        service.verifyVehicle(AUTH_USER_ID, 52L);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(events).publish(eq("driver.vehicle.reviewed"), eq("driver" + AUTH_USER_ID), payload.capture());
        Map<?, ?> event = event(payload.getValue());
        assertEnvelope(event, "VERIFIED");
        assertEquals(52L, event.get("vehicleId"));
        assertTrue(vehicle.getIsVerified());
    }

    @Test
    void suspensionAndReactivationPublishOnlyStateTransitionsWithSafeReason() throws Exception {
        DriverRepository drivers = mock(DriverRepository.class);
        DriverEventPublisherPort events = mock(DriverEventPublisherPort.class);
        DriverModel driver = driver(true);
        when(drivers.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(driver));
        DriverRegistrationService service = new DriverRegistrationService(drivers,
                mock(DriverDocumentRepository.class), mock(VehicleRepository.class),
                mock(DriverEarningRepository.class), mock(DriverEarningMapper.class), events,
                mock(DriverCachePort.class), new ObjectMapper(), mock(DriverCacheService.class),
                mock(DriverNotificationSettingsPort.class));

        service.suspendDriver(AUTH_USER_ID, null);
        service.suspendDriver(AUTH_USER_ID, null);
        assertFalse(driver.isActive());
        service.reactivateDriver(AUTH_USER_ID);
        service.reactivateDriver(AUTH_USER_ID);
        assertTrue(driver.isActive());

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(events, times(2)).publish(eq("driver.account.status.changed"),
                eq("driver" + AUTH_USER_ID), payload.capture());
        Map<?, ?> suspended = event(payload.getAllValues().get(0));
        Map<?, ?> reactivated = event(payload.getAllValues().get(1));
        assertEnvelope(suspended, "SUSPENDED");
        assertEnvelope(reactivated, "REACTIVATED");
        assertEquals("No reason provided.", suspended.get("reason"));
        assertEquals("Account reactivated", reactivated.get("reason"));
    }

    private static DriverModel driver(boolean active) {
        return DriverModel.builder().id(DRIVER_ID).authUserId(AUTH_USER_ID).isActive(active).build();
    }

    private static Map<?, ?> event(Object payload) {
        assertTrue(payload instanceof Map<?, ?>);
        return (Map<?, ?>) payload;
    }

    private static void assertEnvelope(Map<?, ?> event, String expectedStatus) {
        assertEquals(DRIVER_ID, event.get("driverId"));
        assertEquals(AUTH_USER_ID, event.get("authUserId"));
        assertEquals(expectedStatus, event.get("status"));
        UUID.fromString((String) event.get("eventId"));
        Instant.parse((String) event.get("eventTime"));
    }
}
