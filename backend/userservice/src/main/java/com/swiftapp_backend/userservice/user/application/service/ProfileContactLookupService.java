package com.swiftapp_backend.userservice.user.application.service;

import com.swiftapp_backend.userservice.user.domain.repository.UserModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfileContactLookupService {
    private final UserModelRepository repository;
    private final UserService userService;
    public record Contact(Long id, Long authUserId, String fullName, String email, String role, boolean pushNotificationEnabled, boolean emailNotificationEnabled) {}

    @Transactional(readOnly = true)
    public Contact find(Long profileId) {
        var user = repository.findById(profileId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        var preferences = userService.getPreferences(user.getAuthUserId());
        return new Contact(user.getId(), user.getAuthUserId(), user.getFullName(), user.getEmail(),
                "RIDER", preferences.isPushNotificationEnabled(), preferences.isEmailNotificationEnabled());
    }
}
