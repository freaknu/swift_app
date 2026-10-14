package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.ProfileCompletenessDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverEarningMapper;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.domain.enums.Gender;
import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverDocumentRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverEarningRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Rules for GET /api/v1/driver/profile/completeness: a driver is complete
 * only when personal details, all required documents, and at least one
 * valid vehicle are present.
 */
@ExtendWith(MockitoExtension.class)
class ProfileCompletenessServiceTest {

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private DriverDocumentRepository driverDocumentRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DriverEarningRepository driverEarningRepository;
    @Mock
    private DriverEarningMapper driverEarningMapper;
    @Mock
    private DriverEventPublisherPort eventPublisher;
    @Mock
    private DriverCachePort driverCache;
    @Mock
    private DriverCacheService driverCacheService;

    private DriverRegistrationService service;

    private static final long AUTH_USER_ID = 77002L;

    @BeforeEach
    void setUp() {
        service = new DriverRegistrationService(
                driverRepository, driverDocumentRepository, vehicleRepository,
                driverEarningRepository, driverEarningMapper, eventPublisher,
                driverCache, new ObjectMapper(), driverCacheService,
                org.mockito.Mockito.mock(com.swiftapp_backend.driverservice.driver.application.port.DriverNotificationSettingsPort.class));
    }

    private DriverModel kafkaEventDriver() {
        return DriverModel.builder()
                .id(201L)
                .authUserId(AUTH_USER_ID)
                .driverName("Test Driver")
                .email("newdriver@test.com")
                .phoneNumber("9876543210")
                .driverStatus(DriverStatus.OFFLINE)
                .isActive(true)
                .isAcceptingRides(false)
                .isVerified(false)
                .build();
    }

    @Test
    void registrationDocumentsBelongToAuthenticatedDriverAndCountAsComplete() throws Exception {
        DriverModel driver = kafkaEventDriver();
        driver.setGender(Gender.MALE);
        driver.setDateOfBirth(LocalDate.of(1995, 5, 20));
        List<DriverDocumentModel> persisted = new ArrayList<>();
        when(driverRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(DriverModel.class))).thenAnswer(call -> call.getArgument(0));
        when(driverDocumentRepository.saveAll(anyList())).thenAnswer(call -> {
            persisted.addAll(call.getArgument(0));
            return persisted;
        });
        when(driverDocumentRepository.findAllByAuthUserId(AUTH_USER_ID))
                .thenAnswer(call -> persisted.stream()
                        .filter(document -> Long.valueOf(AUTH_USER_ID).equals(document.getAuthUserId()))
                        .toList());
        when(vehicleRepository.findAllByAuthUserId(AUTH_USER_ID)).thenReturn(List.of(
                VehicleModel.builder().vehicleType(VehicleType.SEDAN).brand("Honda")
                        .model("City").registrationNumber("DL01AB1234").isActive(true).build()));

        service.registerDriver(AUTH_USER_ID, DriverCreateRequestDto.builder()
                .allDocuments(Arrays.stream(DocumentType.values())
                        .map(type -> DriverDocumentCreateRequestDto.builder()
                                .documentType(type).documentUrl("https://example.invalid/" + type.name()).build())
                        .toList())
                .build());

        assertEquals(DocumentType.values().length, persisted.size());
        for (DriverDocumentModel document : persisted) {
            assertEquals(AUTH_USER_ID, document.getAuthUserId());
            assertEquals(driver.getId(), document.getDriver().getId());
            assertTrue(document.isActive());
            assertNotNull(document.getUploadedAt());
        }
        assertTrue(service.getProfileCompleteness(AUTH_USER_ID).isComplete());
    }

    @Test
    void kafkaCreatedDriverIsIncomplete() throws Exception {
        when(driverRepository.findByAuthUserId(AUTH_USER_ID))
                .thenReturn(Optional.of(kafkaEventDriver()));
        when(driverDocumentRepository.findAllByAuthUserId(AUTH_USER_ID))
                .thenReturn(List.of());
        when(vehicleRepository.findAllByAuthUserId(AUTH_USER_ID))
                .thenReturn(List.of());

        ProfileCompletenessDto result =
                service.getProfileCompleteness(AUTH_USER_ID);

        assertFalse(result.isComplete());
        assertFalse(result.getPersonal().isComplete());
        assertFalse(result.getDocuments().isComplete());
        assertFalse(result.getVehicle().isComplete());
        assertTrue(result.getPersonal().getMissing().contains("gender"));
        assertTrue(result.getPersonal().getMissing().contains("dateOfBirth"));
        assertTrue(result.getDocuments().getMissing()
                .contains(DocumentType.DRIVING_LICENCE.name()));
        assertTrue(result.getVehicle().getMissing().contains("vehicle"));
    }

    @Test
    void fullyOnboardedDriverIsComplete() throws Exception {
        DriverModel driver = kafkaEventDriver();
        driver.setGender(Gender.MALE);
        driver.setDateOfBirth(LocalDate.of(1995, 5, 20));

        when(driverRepository.findByAuthUserId(AUTH_USER_ID))
                .thenReturn(Optional.of(driver));
        when(driverDocumentRepository.findAllByAuthUserId(AUTH_USER_ID))
                .thenReturn(List.of(
                        DriverDocumentModel.builder()
                                .documentType(DocumentType.DRIVING_LICENCE)
                                .isActive(true).build(),
                        DriverDocumentModel.builder()
                                .documentType(DocumentType.ADHAAR_CARD)
                                .isActive(true).build(),
                        DriverDocumentModel.builder()
                                .documentType(DocumentType.PAN_CARD)
                                .isActive(true).build()));
        when(vehicleRepository.findAllByAuthUserId(AUTH_USER_ID))
                .thenReturn(List.of(
                        VehicleModel.builder()
                                .vehicleType(VehicleType.SEDAN)
                                .brand("Honda")
                                .model("City")
                                .registrationNumber("DL01AB1234")
                                .isActive(true).build()));

        ProfileCompletenessDto result =
                service.getProfileCompleteness(AUTH_USER_ID);

        assertTrue(result.isComplete());
        assertTrue(result.getPersonal().isComplete());
        assertTrue(result.getDocuments().isComplete());
        assertTrue(result.getVehicle().isComplete());
        assertTrue(result.getMissing().isEmpty());
    }

    @Test
    void vehicleMissingRegistrationKeepsVehicleIncomplete() throws Exception {
        DriverModel driver = kafkaEventDriver();
        driver.setGender(Gender.MALE);
        driver.setDateOfBirth(LocalDate.of(1995, 5, 20));

        when(driverRepository.findByAuthUserId(AUTH_USER_ID))
                .thenReturn(Optional.of(driver));
        when(driverDocumentRepository.findAllByAuthUserId(AUTH_USER_ID))
                .thenReturn(List.of(
                        DriverDocumentModel.builder()
                                .documentType(DocumentType.DRIVING_LICENCE)
                                .isActive(true).build(),
                        DriverDocumentModel.builder()
                                .documentType(DocumentType.ADHAAR_CARD)
                                .isActive(true).build(),
                        DriverDocumentModel.builder()
                                .documentType(DocumentType.PAN_CARD)
                                .isActive(true).build()));
        when(vehicleRepository.findAllByAuthUserId(AUTH_USER_ID))
                .thenReturn(List.of(
                        VehicleModel.builder()
                                .vehicleType(VehicleType.SEDAN)
                                .brand("Honda")
                                .model("City")
                                .isActive(true).build()));

        ProfileCompletenessDto result =
                service.getProfileCompleteness(AUTH_USER_ID);

        assertFalse(result.isComplete());
        assertFalse(result.getVehicle().isComplete());
        assertTrue(result.getPersonal().isComplete());
        assertTrue(result.getDocuments().isComplete());
    }

    @Test
    void incompleteDriverCannotGoOnlineButCanGoOffline() throws Exception {
        DriverModel driver = kafkaEventDriver();
        when(driverRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(driver));
        when(driverDocumentRepository.findAllByAuthUserId(AUTH_USER_ID)).thenReturn(List.of());
        when(vehicleRepository.findAllByAuthUserId(AUTH_USER_ID)).thenReturn(List.of());

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.changeStatus(AUTH_USER_ID, DriverStatus.ONLINE));
        verify(driverCacheService, never()).cacheStatusChange(AUTH_USER_ID, DriverStatus.ONLINE);
        service.changeStatus(AUTH_USER_ID, DriverStatus.OFFLINE);
        verify(driverRepository).save(driver);
    }

    @Test
    void suspendedDriverCannotGoOnlineEvenWhenProfileIsComplete() throws Exception {
        DriverModel driver = kafkaEventDriver();
        driver.setActive(false);
        driver.setDriverStatus(DriverStatus.SUSPENDED);
        when(driverRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(driver));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.changeStatus(AUTH_USER_ID, DriverStatus.ONLINE));
        verify(driverCacheService, never()).cacheStatusChange(AUTH_USER_ID, DriverStatus.ONLINE);
    }

    @Test
    void registrationCannotSetManagedAvailabilityOrReactivateSuspendedAccount() {
        DriverModel driver = kafkaEventDriver();
        driver.setActive(false);
        driver.setDriverStatus(DriverStatus.SUSPENDED);
        when(driverRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(DriverModel.class))).thenAnswer(call -> call.getArgument(0));

        service.registerDriver(AUTH_USER_ID, DriverCreateRequestDto.builder()
                .driverStatus(DriverStatus.ONLINE)
                .isAcceptingRides(true)
                .driverAvailability(com.swiftapp_backend.driverservice.driver.domain.enums.DriverAvailability.ONLINE)
                .build());

        assertFalse(driver.isActive());
        assertEquals(DriverStatus.SUSPENDED, driver.getDriverStatus());
        assertFalse(driver.isAcceptingRides());
        assertEquals(null, driver.getDriverAvailability());
    }

    @Test
    void newRegistrationStartsOfflineEvenWithClientOnlineFields() {
        when(driverRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.empty());
        when(driverRepository.save(any(DriverModel.class))).thenAnswer(call -> call.getArgument(0));

        service.registerDriver(AUTH_USER_ID, DriverCreateRequestDto.builder()
                .driverStatus(DriverStatus.ONLINE)
                .isAcceptingRides(true)
                .driverAvailability(com.swiftapp_backend.driverservice.driver.domain.enums.DriverAvailability.ONLINE)
                .build());

        org.mockito.ArgumentCaptor<DriverModel> saved =
                org.mockito.ArgumentCaptor.forClass(DriverModel.class);
        verify(driverRepository).save(saved.capture());
        assertEquals(DriverStatus.OFFLINE, saved.getValue().getDriverStatus());
        assertFalse(saved.getValue().isAcceptingRides());
        assertEquals(null, saved.getValue().getDriverAvailability());
    }
}
