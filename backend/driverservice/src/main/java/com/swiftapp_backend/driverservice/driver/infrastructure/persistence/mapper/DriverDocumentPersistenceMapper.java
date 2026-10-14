package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverDocumentEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;

import java.util.LinkedHashSet;

public final class DriverDocumentPersistenceMapper {
    private DriverDocumentPersistenceMapper() {
    }

    public static DriverDocumentModel toDomain(DriverDocumentEntity source) {
        if (source == null) {
            return null;
        }
        return toDomain(source, DriverPersistenceMapper.toDomainReference(source.getDriver()));
    }

    static DriverDocumentModel toDomain(DriverDocumentEntity source, DriverModel driver) {
        if (source == null) {
            return null;
        }

        DriverDocumentModel target = new DriverDocumentModel();
        target.setId(source.getId());
        target.setDriver(driver);
        target.setDocumentType(source.getDocumentType());
        target.setAuthUserId(source.getAuthUserId());
        target.setActive(source.isActive());
        target.setDocumentUrl(source.getDocumentUrl());
        target.setVerificationStatus(source.getVerificationStatus());
        target.setUploadedAt(source.getUploadedAt());
        target.setVerifiedAt(source.getVerifiedAt());
        target.setVerifiedBy(source.getVerifiedBy());
        target.setRemarks(source.getRemarks());
        target.setAllStatus(source.getAllStatus() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(source.getAllStatus()));
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    public static DriverDocumentEntity toEntity(DriverDocumentModel source, DriverEntity driver) {
        if (source == null) {
            return null;
        }

        DriverDocumentEntity target = new DriverDocumentEntity();
        target.setId(source.getId());
        updateEntity(source, target, driver);
        return target;
    }

    public static void updateEntity(
            DriverDocumentModel source,
            DriverDocumentEntity target,
            DriverEntity driver
    ) {
        target.setDriver(driver);
        target.setDocumentType(source.getDocumentType());
        target.setAuthUserId(source.getAuthUserId());
        target.setActive(source.isActive());
        target.setDocumentUrl(source.getDocumentUrl());
        target.setVerificationStatus(source.getVerificationStatus());
        target.setUploadedAt(source.getUploadedAt());
        target.setVerifiedAt(source.getVerifiedAt());
        target.setVerifiedBy(source.getVerifiedBy());
        target.setRemarks(source.getRemarks());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        if (source.getAllStatus() != null) {
            if (target.getAllStatus() == null) {
                target.setAllStatus(new LinkedHashSet<>(source.getAllStatus()));
            } else {
                target.getAllStatus().clear();
                target.getAllStatus().addAll(source.getAllStatus());
            }
        }
    }
}
