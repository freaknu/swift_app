package com.swiftapp_backend.authservice.authentication.api.controller;

import com.swiftapp_backend.authservice.authentication.api.dto.UserCredentialDto;
import com.swiftapp_backend.authservice.authentication.application.service.UserCredentialService;
import com.swiftapp_backend.authservice.shared.ApiResponseFormat;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.List;

@Tag(name = "User Credential Controller", description = "User Credential Controller")
@RestController
@RequestMapping("/api/v1/user-cred")
@RequiredArgsConstructor
public class UserCredentialController {
    private final UserCredentialService userCredentialService;

    @GetMapping("/getCredentials/{userId}")
    public ResponseEntity<ApiResponseFormat<List<UserCredentialDto>>> getAllCredentials(
            @RequestHeader("x-user-id") Long authUserId, @PathVariable Long userId) {
        if (authUserId == null || !authUserId.equals(userId)) {
            throw new AuthorizationDeniedException("Credentials do not belong to the authenticated user");
        }
        var res = userCredentialService.getAllCredentials(userId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<UserCredentialDto>>builder()
                        .data(res)
                        .message("All Credential Fetched")
                        .success(true)
                        .errors(null)
                        .endpoint("/api/user-cred//getCredentials/" + userId)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }
}
