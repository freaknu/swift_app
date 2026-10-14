package com.swiftapp_backend.paymentservice.payment.domain.model;
import java.math.BigDecimal;
public record CompletedRide(Long rideId, Long riderId, Long driverId, boolean completed,
                            BigDecimal totalFare, String currency, String completedAt) {}
