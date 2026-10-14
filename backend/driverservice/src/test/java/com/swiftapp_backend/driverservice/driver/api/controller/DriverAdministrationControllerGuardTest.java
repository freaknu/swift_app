package com.swiftapp_backend.driverservice.driver.api.controller;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentChangeVerificationStatus;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverSuspensionRequestDto;
import com.swiftapp_backend.driverservice.driver.application.service.DriverCacheService;
import com.swiftapp_backend.driverservice.driver.application.service.DriverDocumentFileStore;
import com.swiftapp_backend.driverservice.driver.application.service.DriverDocumentService;
import com.swiftapp_backend.driverservice.driver.application.service.DriverOfferService;
import com.swiftapp_backend.driverservice.driver.application.service.DriverRegistrationService;
import com.swiftapp_backend.driverservice.driver.application.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DriverAdministrationControllerGuardTest {
    private final DriverDocumentService documents = mock(DriverDocumentService.class);
    private final VehicleService vehicles = mock(VehicleService.class);
    private final DriverRegistrationService drivers = mock(DriverRegistrationService.class);
    private final DocumentController documentController = new DocumentController(
            documents, mock(DriverDocumentFileStore.class));
    private final VehicleController vehicleController = new VehicleController(vehicles);
    private final DriverController driverController = new DriverController(
            drivers, mock(DriverCacheService.class), mock(DriverOfferService.class));

    @Test
    void publicPeerCannotReviewOrReactivateEvenWithDriverIdentityHeader() {
        MockHttpServletRequest remote = requestFrom("203.0.113.8");
        assertThrows(ResponseStatusException.class,
                () -> documentController.changeDocumentStatus(42L,
                        new DriverDocumentChangeVerificationStatus(), remote));
        assertThrows(ResponseStatusException.class,
                () -> vehicleController.verifyVehicle(42L, 10L, remote));
        assertThrows(ResponseStatusException.class,
                () -> driverController.suspendDriver(42L,
                        new DriverSuspensionRequestDto("review"), remote));
        assertThrows(ResponseStatusException.class,
                () -> driverController.reactivateDriver(42L, remote));

        verifyNoInteractions(documents, vehicles, drivers);
    }

    @Test
    void loopbackAdministrativePeerCanPublishReviewState() throws Exception {
        MockHttpServletRequest local = requestFrom("127.0.0.1");
        DriverDocumentChangeVerificationStatus review =
                new DriverDocumentChangeVerificationStatus();

        assertEquals(200, documentController.changeDocumentStatus(42L, review, local).getStatusCode().value());
        assertEquals(200, vehicleController.verifyVehicle(42L, 10L, local).getStatusCode().value());
        assertEquals(200, driverController.suspendDriver(42L,
                new DriverSuspensionRequestDto("review"), local).getStatusCode().value());
        assertEquals(200, driverController.reactivateDriver(42L, local).getStatusCode().value());

        verify(documents).changeDocumentVerificationStatus(42L, review);
        verify(vehicles).verifyVehicle(42L, 10L);
        verify(drivers).suspendDriver(42L, "review");
        verify(drivers).reactivateDriver(42L);
    }

    private MockHttpServletRequest requestFrom(String peer) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        return request;
    }
}
