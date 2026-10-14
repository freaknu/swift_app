package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfileContactLookupService {
    private final DriverRepository repository;
    private final DriverRegistrationService driverService;
    public record Contact(Long id, Long authUserId, String fullName, String email, String role,
                          boolean pushNotificationEnabled, boolean emailNotificationEnabled,
                          boolean rideAlertsEnabled) {}

    @Transactional(readOnly = true)
    public Contact find(Long profileId) {
        var user = repository.findById(profileId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        var preferences = driverService.getNotificationPreferences(user.getAuthUserId());
        return new Contact(user.getId(), user.getAuthUserId(), user.getDriverName(), user.getEmail(),
                "DRIVER", preferences.isPushEnabled(), preferences.isEmailEnabled(),
                preferences.isRideAlertsEnabled());
    }
}
