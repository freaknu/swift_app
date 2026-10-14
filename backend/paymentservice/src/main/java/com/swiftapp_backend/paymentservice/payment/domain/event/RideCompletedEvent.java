package com.swiftapp_backend.paymentservice.payment.domain.event;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideCompletedEvent(Long rideId, Long riderId, Long driverId, BigDecimal totalFare,
                                 String currency, String completedAt) {}
