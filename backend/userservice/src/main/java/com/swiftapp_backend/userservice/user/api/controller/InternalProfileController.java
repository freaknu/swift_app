package com.swiftapp_backend.userservice.user.api.controller;

import com.swiftapp_backend.userservice.user.application.service.ProfileContactLookupService;
import com.swiftapp_backend.userservice.shared.ApiResponseFormat;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetAddress;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/profiles")
public class InternalProfileController {
    private final ProfileContactLookupService profiles;

    @GetMapping("/users/{id}")
    public ApiResponseFormat<ProfileContactLookupService.Contact> get(
            @PathVariable Long id, HttpServletRequest request) {
        requireLoopback(request);
        return ApiResponseFormat.<ProfileContactLookupService.Contact>builder()
                .data(profiles.find(id)).success(true).statusCode(200)
                .message("Profile contact fetched").endpoint(request.getRequestURI()).build();
    }

    static void requireLoopback(HttpServletRequest request) {
        try {
            if (InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) return;
        } catch (Exception ignored) {}
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal endpoint");
    }
}
