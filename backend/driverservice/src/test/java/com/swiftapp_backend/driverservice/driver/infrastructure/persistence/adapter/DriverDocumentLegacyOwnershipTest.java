package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverDocumentEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverDocumentRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DriverDocumentLegacyOwnershipTest {
    @Autowired SpringDataDriverRepository drivers;
    @Autowired SpringDataDriverDocumentRepository documents;
    @Autowired EntityManager entityManager;

    @Test
    void legacyRegistrationUploadIsAccessibleOnlyByItsDriverAccount() {
        DriverEntity driver = new DriverEntity();
        driver.setAuthUserId(81001L);
        driver.setDriverName("Legacy Driver");
        driver.setEmail("legacy-driver@invalid.test");
        driver.setPhoneNumber("9000000001");
        drivers.save(driver);

        DriverDocumentEntity document = new DriverDocumentEntity();
        document.setDriver(driver);
        document.setDocumentType(DocumentType.DRIVING_LICENCE);
        document.setDocumentUrl("https://example.invalid/licence.pdf");
        documents.saveAndFlush(document);
        Long documentId = document.getId();
        entityManager.clear();

        assertThat(documents.findAllByAuthUserId(81001L))
                .extracting(DriverDocumentEntity::getId).containsExactly(documentId);
        assertThat(documents.findByAuthUserIdAndId(81001L, documentId)).isPresent();
        assertThat(documents.findAllByAuthUserId(81002L)).isEmpty();
        assertThat(documents.findByAuthUserIdAndId(81002L, documentId)).isEmpty();
    }
}
