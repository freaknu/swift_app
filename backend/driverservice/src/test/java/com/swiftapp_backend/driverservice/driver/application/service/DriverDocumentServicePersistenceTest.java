package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentResponseDto;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverEarningMapper;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.domain.enums.Gender;
import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter.DriverDocumentRepositoryAdapter;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter.DriverRepositoryAdapter;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter.VehicleRepositoryAdapter;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverDocumentRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataVehicleRepository;
import com.swiftapp_backend.driverservice.shared.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DriverDocumentServicePersistenceTest {
    private static final long AUTH_USER_ID = 83002L;

    @Autowired private SpringDataDriverRepository driverJpa;
    @Autowired private SpringDataDriverDocumentRepository documentJpa;
    @Autowired private SpringDataVehicleRepository vehicleJpa;

    private DriverDocumentService documents;
    private DriverDocumentRepositoryAdapter documentRepository;
    private DriverRepositoryAdapter driverRepository;
    private VehicleRepositoryAdapter vehicleRepository;
    private DriverRegistrationService registration;
    private DriverCachePort cache;
    private DriverModel driver;

    @BeforeEach
    void setUp() {
        driverRepository = new DriverRepositoryAdapter(driverJpa);
        documentRepository = new DriverDocumentRepositoryAdapter(documentJpa, driverJpa);
        vehicleRepository = new VehicleRepositoryAdapter(vehicleJpa, driverJpa);
        cache = mock(DriverCachePort.class);
        DriverEventPublisherPort publisher = mock(DriverEventPublisherPort.class);
        documents = new DriverDocumentService(documentRepository, driverRepository, cache, publisher);
        registration = new DriverRegistrationService(driverRepository, documentRepository,
                vehicleRepository, mock(com.swiftapp_backend.driverservice.driver.domain.repository.DriverEarningRepository.class),
                mock(DriverEarningMapper.class), publisher, cache, new ObjectMapper(),
                mock(DriverCacheService.class),
                mock(com.swiftapp_backend.driverservice.driver.application.port.DriverNotificationSettingsPort.class));
        driver = driverRepository.save(DriverModel.builder()
                .authUserId(AUTH_USER_ID)
                .driverName("Driver")
                .email("driver@example.test")
                .phoneNumber("9876543210")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .driverStatus(DriverStatus.OFFLINE)
                .isActive(true)
                .build());
    }

    @Test
    void uploadPersistsOwnedDocumentAndProfileListsIt() throws Exception {
        DriverDocumentResponseDto uploaded = documents.uploadDocument(AUTH_USER_ID,
                request(DocumentType.DRIVING_LICENCE, "https://example.test/licence"));

        assertNotNull(uploaded.getId());
        assertEquals(driver.getId(), uploaded.getDriverId());
        assertEquals(VerificationStatus.PENDING, uploaded.getVerificationStatus());
        assertNotNull(uploaded.getUploadedAt());
        assertEquals(uploaded.getId(), documents.getAllDocuments(AUTH_USER_ID).getFirst().getId());
        assertEquals(uploaded.getId(), registration.getDriverProfile(AUTH_USER_ID)
                .getAllDocuments().getFirst().getId());
        assertEquals(1, documentRepository.findAllByDriverId(driver.getId()).size());
    }

    @Test
    void repeatUploadReplacesSameDocumentAndResetsApproval() throws Exception {
        DriverDocumentResponseDto first = documents.uploadDocument(AUTH_USER_ID,
                request(DocumentType.PAN_CARD, "https://example.test/old"));
        DriverDocumentModel approved = documentRepository.findById(first.getId()).orElseThrow();
        approved.setVerificationStatus(VerificationStatus.APPROVED);
        approved.setVerifiedAt(LocalDateTime.now());
        approved.setVerifiedBy(17L);
        approved.setRemarks("Approved");
        approved.setAllStatus(new LinkedHashSet<>(List.of(VerificationStatus.APPROVED)));
        documentRepository.save(approved);

        DriverDocumentResponseDto replaced = documents.uploadDocument(AUTH_USER_ID,
                request(DocumentType.PAN_CARD, "https://example.test/new"));

        assertEquals(first.getId(), replaced.getId());
        assertEquals("https://example.test/new", replaced.getDocumentUrl());
        assertEquals(VerificationStatus.PENDING, replaced.getVerificationStatus());
        assertNull(replaced.getVerifiedAt());
        assertNull(replaced.getRemarks());
        assertEquals(List.of(VerificationStatus.PENDING),
                documents.getDocumentStatus(AUTH_USER_ID, first.getId()));
        assertEquals(1, documents.getAllDocuments(AUTH_USER_ID).size());
        assertEquals(1, documentJpa.count());
    }

    @Test
    void storedFileReadRequiresActiveOwnedDocument() throws Exception {
        UUID fileId = UUID.randomUUID();
        DriverDocumentResponseDto uploaded = documents.uploadDocument(AUTH_USER_ID,
                request(DocumentType.PAN_CARD, "/api/v1/document/files/" + fileId));

        assertTrue(documents.ownsActiveFile(AUTH_USER_ID, fileId));
        assertFalse(documents.ownsActiveFile(999L, fileId));
        documents.deleteDocument(uploaded.getId(), AUTH_USER_ID);
        assertFalse(documents.ownsActiveFile(AUTH_USER_ID, fileId));
    }

    @Test
    void legacyUploadWithoutDriverRelationIsListedAndRepairedOnReplace() throws Exception {
        DriverDocumentModel legacy = documentRepository.save(DriverDocumentModel.builder()
                .authUserId(AUTH_USER_ID)
                .documentType(DocumentType.ADHAAR_CARD)
                .documentUrl("https://example.test/legacy")
                .verificationStatus(VerificationStatus.PENDING)
                .isActive(true)
                .uploadedAt(LocalDateTime.now())
                .build());

        assertEquals(legacy.getId(), documents.getAllDocuments(AUTH_USER_ID).getFirst().getId());
        DriverDocumentResponseDto replaced = documents.uploadDocument(AUTH_USER_ID,
                request(DocumentType.ADHAAR_CARD, "https://example.test/current"));

        assertEquals(legacy.getId(), replaced.getId());
        assertEquals(driver.getId(), replaced.getDriverId());
        assertEquals(1, documentJpa.count());
    }

    @Test
    void unreadableLegacyProfileCacheIsEvictedAndDocumentsReload() throws Exception {
        DriverDocumentResponseDto uploaded = documents.uploadDocument(AUTH_USER_ID,
                request(DocumentType.DRIVING_LICENCE, "https://example.test/licence"));
        when(cache.exists("driver" + AUTH_USER_ID)).thenReturn(true);
        when(cache.get("driver" + AUTH_USER_ID))
                .thenThrow(new SerializationException("legacy profile payload"));

        assertEquals(uploaded.getId(), registration.getDriverProfile(AUTH_USER_ID)
                .getAllDocuments().getFirst().getId());
        verify(cache, org.mockito.Mockito.atLeastOnce()).delete("driver" + AUTH_USER_ID);
    }

    @Test
    void documentAndVehicleUploadsCompleteProfileAndDeletionReopensGap() throws Exception {
        vehicleRepository.save(VehicleModel.builder()
                .driver(driver).authUserId(AUTH_USER_ID).vehicleType(VehicleType.SEDAN)
                .brand("Honda").model("City").registrationNumber("KA01AB1234")
                .year(2022).isActive(true).build());
        for (DocumentType type : DocumentType.values()) {
            documents.uploadDocument(AUTH_USER_ID,
                    request(type, "https://example.test/" + type));
        }

        assertTrue(registration.getProfileCompleteness(AUTH_USER_ID).isComplete());
        Long removedId = documents.getAllDocuments(AUTH_USER_ID).getFirst().getId();
        assertThrows(ApiException.class, () -> documents.deleteDocument(removedId, 999L));
        assertTrue(registration.getProfileCompleteness(AUTH_USER_ID).isComplete());

        documents.deleteDocument(removedId, AUTH_USER_ID);
        assertFalse(registration.getProfileCompleteness(AUTH_USER_ID).isComplete());
        assertEquals(2, documents.getAllDocuments(AUTH_USER_ID).size());
    }

    private static DriverDocumentCreateRequestDto request(DocumentType type, String url) {
        return DriverDocumentCreateRequestDto.builder()
                .documentType(type).documentUrl(url).build();
    }
}
