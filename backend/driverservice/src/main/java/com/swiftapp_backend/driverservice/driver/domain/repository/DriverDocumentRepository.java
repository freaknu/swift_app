package com.swiftapp_backend.driverservice.driver.domain.repository;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;

import java.util.List;
import java.util.Optional;

public interface DriverDocumentRepository {

    Optional<DriverDocumentModel> findById(Long id);

    List<DriverDocumentModel> findAllByDriverId(Long driverId);

    List<DriverDocumentModel> findAllByAuthUserId(Long authUserId);

    Optional<DriverDocumentModel> findByAuthUserIdAndId(Long authUserId, Long id);

    DriverDocumentModel save(DriverDocumentModel document);

    List<DriverDocumentModel> saveAll(Iterable<DriverDocumentModel> documents);
}
