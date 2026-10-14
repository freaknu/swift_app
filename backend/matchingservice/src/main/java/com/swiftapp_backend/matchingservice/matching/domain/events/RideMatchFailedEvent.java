package com.swiftapp_backend.matchingservice.matching.domain.events;

public record RideMatchFailedEvent(Long rideId, Long riderId, String reason) {}
