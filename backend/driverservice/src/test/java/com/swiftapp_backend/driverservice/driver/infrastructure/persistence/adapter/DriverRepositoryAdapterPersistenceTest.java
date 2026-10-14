package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DriverRepositoryAdapter.class)
class DriverRepositoryAdapterPersistenceTest {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void cascadedGraphRetainsDefaultsBackReferencesAndMergeUpdates() {
        DriverModel driver = new DriverModel();
        driver.setAuthUserId(101L);
        driver.setDriverName("Driver One");
        driver.setEmail("driver@example.com");
        driver.setPhoneNumber("555-0101");
        driver.setLanguages(new ArrayList<>());
        driver.setDriverPreferences(new ArrayList<>());

        VehicleModel vehicle = new VehicleModel();
        vehicle.setDriver(driver);
        vehicle.setAuthUserId(101L);
        vehicle.setBrand("Before");
        vehicle.setVehicleImage(new ArrayList<>(List.of("front.jpg")));
        driver.setAllVehicles(new ArrayList<>(List.of(vehicle)));

        DriverDocumentModel document = new DriverDocumentModel();
        document.setDriver(driver);
        document.setAuthUserId(101L);
        document.setDocumentType(DocumentType.DRIVING_LICENCE);
        document.setDocumentUrl("licence.jpg");
        document.setAllStatus(new LinkedHashSet<>(List.of(VerificationStatus.PENDING)));
        driver.setAllDocuments(new ArrayList<>(List.of(document)));

        DriverModel saved = driverRepository.save(driver);
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isAcceptingRides()).isFalse();
        assertThat(saved.isVerified()).isFalse();
        assertThat(saved.getAllVehicles().getFirst().isActive()).isTrue();
        assertThat(saved.getAllVehicles().getFirst().getIsVerified()).isFalse();
        assertThat(saved.getAllDocuments().getFirst().isActive()).isTrue();
        assertThat(saved.getAllDocuments().getFirst().getVerificationStatus())
                .isEqualTo(VerificationStatus.PENDING);

        DriverModel loaded = driverRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getAllVehicles().getFirst().getDriver()).isSameAs(loaded);
        assertThat(loaded.getAllDocuments().getFirst().getDriver()).isSameAs(loaded);

        loaded.setDriverName("Driver Updated");
        loaded.getAllVehicles().getFirst().setBrand("After");
        loaded.getAllDocuments().getFirst().setRemarks("Reviewed");
        driverRepository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        DriverModel updated = driverRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getDriverName()).isEqualTo("Driver Updated");
        assertThat(updated.getAllVehicles().getFirst().getBrand()).isEqualTo("After");
        assertThat(updated.getAllDocuments().getFirst().getRemarks()).isEqualTo("Reviewed");
        assertThat(updated.getAllVehicles().getFirst().getDriver()).isSameAs(updated);
        assertThat(updated.getAllDocuments().getFirst().getDriver()).isSameAs(updated);
    }
}
