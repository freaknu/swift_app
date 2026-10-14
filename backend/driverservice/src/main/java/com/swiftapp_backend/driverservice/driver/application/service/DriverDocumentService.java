package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentChangeVerificationStatus;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentResponseDto;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverDocumentMapper;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverDocumentRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.shared.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverDocumentService {
    private final DriverDocumentRepository driverDocumentRepository;
    private final DriverRepository driverRepository;
    private final DriverCachePort driverCache;
    private final DriverEventPublisherPort eventPublisher;

    // for admin use only
    @Transactional(rollbackFor = Exception.class)
    public void changeDocumentVerificationStatus(Long authUserId, DriverDocumentChangeVerificationStatus req) throws Exception {
        Optional<DriverDocumentModel> existingDocument = driverDocumentRepository.findByAuthUserIdAndId(authUserId,req.getDocumentId());
        if(existingDocument.isEmpty() || !existingDocument.get().isActive()) throw new Exception("Document Don't exists");
        VerificationStatus previousStatus = existingDocument.get().getVerificationStatus();
        boolean statusChanged = req.getVerificationStatus() != null
                && req.getVerificationStatus() != previousStatus;
        Long driverId = statusChanged ? driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> ApiException.notFound("Driver not found")).getId() : null;
        existingDocument.get().setVerificationStatus(req.getVerificationStatus());
        existingDocument.get().setRemarks(req.getRemarks());
        existingDocument.get().getAllStatus().add(req.getVerificationStatus());

        if(req.getVerificationStatus() == VerificationStatus.APPROVED) {
            existingDocument.get().setVerifiedBy(req.getVerifiedBy());
            existingDocument.get().setVerifiedAt(LocalDateTime.now());
        }

        // saving the document status
        driverDocumentRepository.save(existingDocument.get());
        invalidateProfile(authUserId);
        eventPublisher.publish("driver.document.uploaded", "driver" + authUserId, "Document status updated: " + req.getVerificationStatus());
        if (statusChanged) {
            String reason = req.getRemarks() == null || req.getRemarks().isBlank()
                    ? "No review details provided." : req.getRemarks().trim();
            eventPublisher.publish("driver.document.reviewed", "driver" + authUserId,
                    Map.of("eventId", UUID.randomUUID().toString(),
                            "driverId", driverId,
                            "authUserId", authUserId,
                            "status", req.getVerificationStatus().name(),
                            "eventTime", Instant.now().toString(),
                            "documentId", existingDocument.get().getId(),
                            "documentType", existingDocument.get().getDocumentType().name(),
                            "reason", reason));
        }
    }

    @Transactional(readOnly = true)
    public List<VerificationStatus> getDocumentStatus(Long authUserId,Long documentId) throws Exception {
        Optional<DriverDocumentModel> existingDocument = driverDocumentRepository.findByAuthUserIdAndId(authUserId,documentId);
        if(existingDocument.isEmpty() || !existingDocument.get().isActive()) throw new Exception("Document Not available");

        return new ArrayList<>(existingDocument.get().getAllStatus());
    }


    @Transactional(rollbackFor = Exception.class)
    public DriverDocumentResponseDto uploadDocument(Long authUserId,
                                                    DriverDocumentCreateRequestDto data) throws Exception {
        DriverModel driver = driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> ApiException.notFound("Driver not found"));
        List<DriverDocumentModel> existing = activeDocumentsOfType(authUserId,
                data.getDocumentType());
        DriverDocumentModel document = existing.stream()
                .max(Comparator.comparing(DriverDocumentModel::getUploadedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseGet(DriverDocumentModel::new);
        replaceDocument(document, driver, authUserId, data);
        DriverDocumentModel saved = driverDocumentRepository.save(document);
        for (DriverDocumentModel duplicate : existing) {
            if (!duplicate.getId().equals(saved.getId())) {
                duplicate.setActive(false);
                driverDocumentRepository.save(duplicate);
            }
        }
        invalidateProfile(authUserId);
        DriverDocumentResponseDto response = DriverDocumentMapper.toResponse(saved);
        eventPublisher.publish("driver.document.uploaded", "driver" + authUserId, response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<DriverDocumentResponseDto> getAllDocuments(Long authUserId) {
        List<DriverDocumentModel> owned = driverDocumentRepository.findAllByAuthUserId(authUserId);
        return Arrays.stream(DocumentType.values())
                .map(type -> owned.stream()
                        .filter(document -> document.isActive()
                                && document.getDocumentType() == type)
                        .max(Comparator.comparing(DriverDocumentModel::getUploadedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))))
                .flatMap(Optional::stream)
                .map(DriverDocumentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean ownsActiveFile(Long authUserId, UUID fileId) {
        String url = "/api/v1/document/files/" + fileId;
        return getAllDocuments(authUserId).stream()
                .anyMatch(document -> url.equals(document.getDocumentUrl()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId,Long authUserId) throws Exception {
        Optional<DriverDocumentModel> existingDocument =
                driverDocumentRepository.findByAuthUserIdAndId(authUserId, documentId);
        if(existingDocument.isEmpty() || !existingDocument.get().isActive())
            throw ApiException.notFound("Document not available");
        existingDocument.get().setActive(false);
        driverDocumentRepository.save(existingDocument.get());
        invalidateProfile(authUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(Long authUserId,Long documentId,DriverDocumentCreateRequestDto data) throws Exception {
        Optional<DriverDocumentModel> existingDocument = driverDocumentRepository.findByAuthUserIdAndId(authUserId,documentId);
        if(existingDocument.isEmpty() || !existingDocument.get().isActive())
            throw ApiException.notFound("Document not available");
        if(data.getDocumentType() != existingDocument.get().getDocumentType())
            throw ApiException.badRequest("Document type cannot be changed");
        DriverModel driver = driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> ApiException.notFound("Driver not found"));
        replaceDocument(existingDocument.get(), driver, authUserId, data);
        DriverDocumentModel saved = driverDocumentRepository.save(existingDocument.get());
        invalidateProfile(authUserId);
        eventPublisher.publish("driver.document.uploaded", "driver" + authUserId,
                DriverDocumentMapper.toResponse(saved));
    }

    private List<DriverDocumentModel> activeDocumentsOfType(Long authUserId, DocumentType type) {
        return driverDocumentRepository.findAllByAuthUserId(authUserId).stream()
                .filter(document -> document.isActive() && document.getDocumentType() == type)
                .toList();
    }

    private void replaceDocument(DriverDocumentModel document, DriverModel driver,
                                 Long authUserId, DriverDocumentCreateRequestDto data) {
        LocalDateTime now = LocalDateTime.now();
        document.setDriver(driver);
        document.setAuthUserId(authUserId);
        document.setDocumentType(data.getDocumentType());
        document.setDocumentUrl(data.getDocumentUrl());
        document.setActive(true);
        document.setVerificationStatus(VerificationStatus.PENDING);
        document.setVerifiedAt(null);
        document.setVerifiedBy(null);
        document.setRemarks(null);
        document.setUploadedAt(now);
        document.setUpdatedAt(now);
        document.setAllStatus(new LinkedHashSet<>(List.of(VerificationStatus.PENDING)));
    }

    private void invalidateProfile(Long authUserId) {
        driverCache.delete("driver" + authUserId);
    }
}
