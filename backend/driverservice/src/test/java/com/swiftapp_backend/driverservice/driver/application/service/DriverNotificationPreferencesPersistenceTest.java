package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverNotificationPreferencesDto;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverEarningMapper;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverNotificationSettings;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter.DriverNotificationSettingsAdapter;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter.DriverRepositoryAdapter;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverNotificationPreferenceRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DriverNotificationPreferencesPersistenceTest {
    private static final long AUTH_A = 94001L;
    private static final long AUTH_B = 94002L;

    @Autowired private SpringDataDriverRepository driverJpa;
    @Autowired private SpringDataDriverNotificationPreferenceRepository preferenceJpa;

    private DriverRepositoryAdapter drivers;
    private DriverNotificationSettingsAdapter settings;
    private DriverCachePort cache;
    private DriverRegistrationService service;
    private DriverModel driverA;
    private DriverModel driverB;

    @BeforeEach
    void setUp() {
        drivers = new DriverRepositoryAdapter(driverJpa);
        settings = new DriverNotificationSettingsAdapter(preferenceJpa, driverJpa);
        cache = mock(DriverCachePort.class);
        service = serviceWith(cache);
        driverA = drivers.save(DriverModel.builder()
                .authUserId(AUTH_A).driverName("Driver A").email("a@example.test")
                .phoneNumber("9000000001").isActive(true).build());
        driverB = drivers.save(DriverModel.builder()
                .authUserId(AUTH_B).driverName("Driver B").email("b@example.test")
                .phoneNumber("9000000002").isActive(true).build());
    }

    @Test
    void optOutSurvivesCacheEvictionAndServiceReload() {
        DriverNotificationPreferencesDto optOut = DriverNotificationPreferencesDto.builder()
                .pushEnabled(false).smsEnabled(false).emailEnabled(false)
                .rideAlertsEnabled(false).surgeAlertsEnabled(true).build();

        service.updateNotificationPreferences(AUTH_A, optOut);
        assertEquals(1, preferenceJpa.count());
        var row = preferenceJpa.findByDriver_Id(driverA.getId()).orElseThrow();
        assertEquals(AUTH_A, row.getAuthUserId());
        assertFalse(row.isPushEnabled());
        assertFalse(row.isEmailEnabled());
        assertFalse(row.isRideRequestEnabled());
        assertTrue(row.isPromotionEnabled());

        DriverCachePort emptyCache = mock(DriverCachePort.class);
        DriverNotificationPreferencesDto reloaded = serviceWith(emptyCache)
                .getNotificationPreferences(AUTH_A);
        assertFalse(reloaded.isPushEnabled());
        assertFalse(reloaded.isSmsEnabled());
        assertFalse(reloaded.isEmailEnabled());
        assertFalse(reloaded.isRideAlertsEnabled());
        assertTrue(reloaded.isSurgeAlertsEnabled());
        verifyNoInteractions(emptyCache);
    }

    @Test
    void legacyRedisSettingsPromoteOnFirstReadAndStayDurable() {
        String key = "driverNotifPref:" + AUTH_A;
        when(cache.exists(key)).thenReturn(true);
        when(cache.get(key)).thenReturn(Map.of(
                "pushEnabled", false, "smsEnabled", true, "emailEnabled", false,
                "rideAlertsEnabled", false, "surgeAlertsEnabled", true));

        DriverNotificationPreferencesDto first = service.getNotificationPreferences(AUTH_A);
        assertFalse(first.isPushEnabled());
        assertFalse(first.isEmailEnabled());
        assertEquals(1, preferenceJpa.count());
        verify(cache).delete(key);

        DriverCachePort emptyCache = mock(DriverCachePort.class);
        DriverNotificationPreferencesDto afterRestart = serviceWith(emptyCache)
                .getNotificationPreferences(AUTH_A);
        assertFalse(afterRestart.isPushEnabled());
        assertFalse(afterRestart.isEmailEnabled());
        verifyNoInteractions(emptyCache);
    }

    @Test
    void rejectsForeignDriverAssociationAndLeavesOtherDriverUnchanged() {
        assertThrows(IllegalArgumentException.class, () -> settings.save(
                new DriverNotificationSettings(driverB.getId(), AUTH_A,
                        false, false, false, false, false)));
        assertEquals(0, preferenceJpa.count());
        DriverNotificationPreferencesDto other = service.getNotificationPreferences(AUTH_B);
        assertTrue(other.isPushEnabled());
        assertTrue(other.isEmailEnabled());
    }

    private DriverRegistrationService serviceWith(DriverCachePort cachePort) {
        return new DriverRegistrationService(drivers,
                mock(com.swiftapp_backend.driverservice.driver.domain.repository.DriverDocumentRepository.class),
                mock(com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository.class),
                mock(com.swiftapp_backend.driverservice.driver.domain.repository.DriverEarningRepository.class),
                mock(DriverEarningMapper.class), mock(DriverEventPublisherPort.class),
                cachePort, new ObjectMapper(), mock(DriverCacheService.class), settings);
    }
}
