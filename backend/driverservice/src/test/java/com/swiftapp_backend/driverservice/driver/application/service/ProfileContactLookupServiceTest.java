package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverNotificationPreferencesDto;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileContactLookupServiceTest {
    @Test
    void exposesGlobalPushAndRideAlertsSeparatelyForNotificationCategories() {
        DriverRepository drivers = mock(DriverRepository.class);
        DriverRegistrationService registration = mock(DriverRegistrationService.class);
        ProfileContactLookupService service = new ProfileContactLookupService(drivers, registration);
        when(drivers.findById(23L)).thenReturn(Optional.of(DriverModel.builder()
                .id(23L).authUserId(8L).driverName("Driver").email("driver@example.com")
                .build()));
        when(registration.getNotificationPreferences(8L)).thenReturn(
                DriverNotificationPreferencesDto.builder()
                        .pushEnabled(true).emailEnabled(false).rideAlertsEnabled(false).build(),
                DriverNotificationPreferencesDto.builder()
                        .pushEnabled(false).emailEnabled(true).rideAlertsEnabled(true).build());

        var rideAlertsOff = service.find(23L);
        assertThat(rideAlertsOff.pushNotificationEnabled()).isTrue();
        assertThat(rideAlertsOff.rideAlertsEnabled()).isFalse();
        assertThat(rideAlertsOff.emailNotificationEnabled()).isFalse();

        var globalPushOff = service.find(23L);
        assertThat(globalPushOff.pushNotificationEnabled()).isFalse();
        assertThat(globalPushOff.rideAlertsEnabled()).isTrue();
        assertThat(globalPushOff.emailNotificationEnabled()).isTrue();
    }
}
