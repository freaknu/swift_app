package com.swiftapp_backend.driverservice.driver.api.dto;

/** Ride-scoped public driver details; excludes private contact and documents. */
public record AssignedDriverSummaryDto(Long driverId, String driverName, String profileImage,
                                       Long vehicleId, String vehicleType, String brand,
                                       String model, String color, String registrationNumber,
                                       Integer seatCapacity, Boolean vehicleVerified) {}
