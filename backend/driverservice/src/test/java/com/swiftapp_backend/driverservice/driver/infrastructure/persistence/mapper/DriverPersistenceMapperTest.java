package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper;

import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverDocumentEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.VehicleEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DriverPersistenceMapperTest {

    @Test
    void roundTripPreservesChildBackReferencesWithoutRecursing() {
        DriverEntity driver = new DriverEntity();
        driver.setId(17L);
        driver.setAuthUserId(71L);
        driver.setDriverName("Driver One");
        driver.setLanguages(new ArrayList<>());
        driver.setDriverPreferences(new ArrayList<>());

        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setId(23L);
        vehicle.setDriver(driver);
        vehicle.setBrand("Example");
        vehicle.setVehicleImage(new ArrayList<>(List.of("front.jpg")));
        driver.setAllVehicles(new ArrayList<>(List.of(vehicle)));

        DriverDocumentEntity document = new DriverDocumentEntity();
        document.setId(29L);
        document.setDriver(driver);
        document.setDocumentType(DocumentType.DRIVING_LICENCE);
        document.setVerificationStatus(VerificationStatus.PENDING);
        document.setAllStatus(new LinkedHashSet<>(List.of(VerificationStatus.PENDING)));
        driver.setAllDocuments(new ArrayList<>(List.of(document)));

        DriverModel domain = DriverPersistenceMapper.toDomain(driver);

        assertThat(domain.getAllVehicles()).hasSize(1);
        assertThat(domain.getAllDocuments()).hasSize(1);
        assertThat(domain.getAllVehicles().getFirst().getDriver()).isSameAs(domain);
        assertThat(domain.getAllDocuments().getFirst().getDriver()).isSameAs(domain);

        DriverEntity roundTrip = DriverPersistenceMapper.toEntity(domain);

        assertThat(roundTrip.getAllVehicles()).hasSize(1);
        assertThat(roundTrip.getAllDocuments()).hasSize(1);
        assertThat(roundTrip.getAllVehicles().getFirst().getDriver()).isSameAs(roundTrip);
        assertThat(roundTrip.getAllDocuments().getFirst().getDriver()).isSameAs(roundTrip);
        assertThat(roundTrip.getAllDocuments().getFirst().getAllStatus())
                .containsExactly(VerificationStatus.PENDING);
    }

    @Test
    void updateReusesExistingChildrenAndRestoresTheirBackReferences() {
        DriverEntity existing = new DriverEntity();
        existing.setId(17L);
        existing.setLanguages(new ArrayList<>());
        existing.setDriverPreferences(new ArrayList<>());

        VehicleEntity existingVehicle = new VehicleEntity();
        existingVehicle.setId(23L);
        existingVehicle.setDriver(existing);
        existing.setAllVehicles(new ArrayList<>(List.of(existingVehicle)));

        DriverDocumentEntity existingDocument = new DriverDocumentEntity();
        existingDocument.setId(29L);
        existingDocument.setDriver(existing);
        existingDocument.setAllStatus(new LinkedHashSet<>());
        existing.setAllDocuments(new ArrayList<>(List.of(existingDocument)));

        DriverModel changed = DriverPersistenceMapper.toDomain(existing);
        changed.getAllVehicles().getFirst().setBrand("Updated");
        changed.getAllDocuments().getFirst().setRemarks("Reviewed");

        DriverPersistenceMapper.updateEntity(changed, existing);

        assertThat(existing.getAllVehicles().getFirst()).isSameAs(existingVehicle);
        assertThat(existing.getAllDocuments().getFirst()).isSameAs(existingDocument);
        assertThat(existingVehicle.getDriver()).isSameAs(existing);
        assertThat(existingDocument.getDriver()).isSameAs(existing);
        assertThat(existingVehicle.getBrand()).isEqualTo("Updated");
        assertThat(existingDocument.getRemarks()).isEqualTo("Reviewed");
    }
}
