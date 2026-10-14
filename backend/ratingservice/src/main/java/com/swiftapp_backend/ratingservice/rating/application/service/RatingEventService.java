package com.swiftapp_backend.ratingservice.rating.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RatingEventService {
    public void handleRideCompleted(Long rideId, Long driverId, Long riderId) {
        log.debug("Ride completed and available for rating: rideId={}, driverId={}, riderId={}",
                rideId, driverId, riderId);
    }

    public void handleDriverCreated(Long driverId) {
        log.debug("Driver rating profile event received: driverId={}", driverId);
    }

    public void handleUserCreated(Long userId) {
        log.debug("Rider rating profile event received: userId={}", userId);
    }
}
