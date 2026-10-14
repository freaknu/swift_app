package com.swiftapp_backend.riderservice.ride.api.dto.ride;

public record AssignedDriverSummaryDto(Long driverId, String driverName, String profileImage,
                                       Long vehicleId, String vehicleType, String brand,
                                       String model, String color, String registrationNumber,
                                       Integer seatCapacity, Boolean vehicleVerified) {}
