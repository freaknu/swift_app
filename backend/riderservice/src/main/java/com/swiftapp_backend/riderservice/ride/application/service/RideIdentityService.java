package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.application.port.RideIdentityPort;
import com.swiftapp_backend.riderservice.shared.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideIdentityService {
    private final RideIdentityPort identityPort;

    public void requireParticipant(Long authUserId, Long riderId, Long driverId) {
        if (authUserId != null && ((riderId != null && authUserId.equals(identityPort.riderAuthUserId(riderId)))
                || (driverId != null && authUserId.equals(identityPort.driverAuthUserId(driverId))))) return;
        throw ApiException.forbidden("Ride does not belong to the authenticated user");
    }

    public void requireRider(Long authUserId, Long riderId) {
        if (authUserId == null || riderId == null
                || !riderId.equals(identityPort.riderIdForAuthUser(authUserId))) {
            throw ApiException.forbidden("Authenticated rider does not own this profile");
        }
    }

    public void requireDriver(Long authUserId, Long driverId) {
        if (authUserId == null || driverId == null
                || !driverId.equals(identityPort.driverIdForAuthUser(authUserId))) {
            throw ApiException.forbidden("Authenticated driver does not own this profile");
        }
    }

    public Long driverIdForAuthUser(Long authUserId) {
        if (authUserId == null || authUserId <= 0) {
            throw ApiException.forbidden("Authenticated driver is required");
        }
        Long driverId = identityPort.driverIdForAuthUser(authUserId);
        if (driverId == null || driverId <= 0) {
            throw ApiException.forbidden("Authenticated driver profile was not found");
        }
        return driverId;
    }
}
