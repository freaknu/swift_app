package com.swiftapp_backend.driverservice.driver.api.controller;

import com.swiftapp_backend.driverservice.driver.api.dto.*;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import com.swiftapp_backend.driverservice.driver.application.service.DriverDocumentService;
import com.swiftapp_backend.driverservice.driver.application.service.DriverDocumentFileStore;
import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.shared.common.ApiException;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import com.swiftapp_backend.driverservice.shared.common.ApiResponseFormat;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@Tag(name = "Document Controller",description = "Apis for Document Management")
@RestController
@RequestMapping("/api/v1/document")
@RequiredArgsConstructor
public class DocumentController {
    private final DriverDocumentService driverDocumentService;
    private final DriverDocumentFileStore fileStore;

    @GetMapping
    @Operation(summary = "List the authenticated driver's active documents")
    public ResponseEntity<ApiResponseFormat<List<DriverDocumentResponseDto>>> getDocuments(
            @RequestHeader("x-user-id") Long userId) {
        return ResponseEntity.ok(ok(driverDocumentService.getAllDocuments(userId),
                "Documents fetched successfully", "/api/v1/document"));
    }

    @Operation(description = "Fetch all Document status",method = "GET")
    @GetMapping("/getDocumentStatus/{documentId}")
    public ResponseEntity<ApiResponseFormat<List<VerificationStatus>>> getDocumentStatus(@RequestHeader("x-user-id") Long authUserId,
                                                                                         @PathVariable Long documentId) throws Exception {
        var res = driverDocumentService.getDocumentStatus(authUserId,documentId);
        return ResponseEntity.ok()
                .body(ok(res,"Document status fetched successfully","/api/v1/document/getDocumentStatus/" + documentId));
    }



    @PostMapping("/upload")
    public ResponseEntity<ApiResponseFormat<DriverDocumentResponseDto>> uploadDocument(
            @RequestHeader("x-user-id") Long userId,
            @Valid @RequestBody DriverDocumentCreateRequestDto data) throws Exception {
        var res = driverDocumentService.uploadDocument(userId, data);
        return ResponseEntity.ok()
                .body(ok(res, "Document Uploaded Successfully", "/api/v1/document/upload"));
    }

    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace the authenticated driver's document file")
    public ResponseEntity<ApiResponseFormat<DriverDocumentResponseDto>> uploadFile(
            @RequestHeader("x-user-id") Long userId,
            @RequestParam DocumentType documentType,
            @RequestParam("file") MultipartFile file) throws Exception {
        String previousUrl = driverDocumentService.getAllDocuments(userId).stream()
                .filter(document -> document.getDocumentType() == documentType)
                .map(DriverDocumentResponseDto::getDocumentUrl)
                .findFirst().orElse(null);
        DriverDocumentFileStore.Stored stored = fileStore.store(file);
        try {
            DriverDocumentResponseDto response = driverDocumentService.uploadDocument(userId,
                    DriverDocumentCreateRequestDto.builder()
                            .documentType(documentType).documentUrl(stored.url()).build());
            fileStore.deleteIfStoredUrl(previousUrl);
            return ResponseEntity.ok(ok(response, "Document Uploaded Successfully",
                    "/api/v1/document/upload-file"));
        } catch (Exception ex) {
            fileStore.deleteIfStoredUrl(stored.url());
            throw ex;
        }
    }

    @GetMapping("/files/{fileId}")
    @Operation(summary = "Download an active document file owned by the authenticated driver")
    public ResponseEntity<Resource> getFile(@RequestHeader("x-user-id") Long userId,
                                            @PathVariable UUID fileId) {
        if (!driverDocumentService.ownsActiveFile(userId, fileId)) {
            throw ApiException.notFound("Document file not found");
        }
        DriverDocumentFileStore.FileContent stored = fileStore.load(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=driver-document-" + fileId)
                .body(stored.resource());
    }

    @PostMapping("/changeDocumentStatus")
    public ResponseEntity<ApiResponseFormat<Object>> changeDocumentStatus(@RequestHeader("x-user-id") Long userId,
                                                                           @RequestBody DriverDocumentChangeVerificationStatus data,
                                                                           HttpServletRequest request) throws Exception {
        InternalProfileController.requireLoopback(request);
        driverDocumentService.changeDocumentVerificationStatus(userId,data);
        return ResponseEntity.ok()
                .body(ok(null,"Document Status Updated Successfully","/api/v1/document/changeDocumentStatus"));
    }

    @PutMapping("/updateDocument/{documentId}")
    public ResponseEntity<ApiResponseFormat<Object>> updateDocument(@RequestHeader("x-user-id")Long userId,
                                                                    @PathVariable Long documentId,
                                                                    @Valid @RequestBody DriverDocumentCreateRequestDto data) throws Exception {
        String previousUrl = driverDocumentService.getAllDocuments(userId).stream()
                .filter(document -> documentId.equals(document.getId()))
                .map(DriverDocumentResponseDto::getDocumentUrl).findFirst().orElse(null);
        driverDocumentService.updateDocument(userId,documentId,data);
        if (!data.getDocumentUrl().equals(previousUrl)) fileStore.deleteIfStoredUrl(previousUrl);
        return ResponseEntity.ok()
                .body(ok(null,"Document Updated Successfully","/api/v1/document/updateDocument/" + documentId));
    }

    @DeleteMapping("/deleteDocument/{documentId}")
    public ResponseEntity<ApiResponseFormat<Object>> deleteDocument(@RequestHeader("x-user-id")Long userId,
                                                                    @PathVariable Long documentId) throws Exception {
        String previousUrl = driverDocumentService.getAllDocuments(userId).stream()
                .filter(document -> documentId.equals(document.getId()))
                .map(DriverDocumentResponseDto::getDocumentUrl).findFirst().orElse(null);
        driverDocumentService.deleteDocument(documentId,userId);
        fileStore.deleteIfStoredUrl(previousUrl);
        return ResponseEntity.ok()
                .body(ok(null,"Document Deleted Successfully","/api/v1/document/deleteDocument/" + documentId));
    }
    private <T> ApiResponseFormat<T> ok(T data, String message, String endpoint) {
        return ApiResponseFormat.<T>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .endpoint(endpoint)
                .errors(null)
                .build();
    }
}
