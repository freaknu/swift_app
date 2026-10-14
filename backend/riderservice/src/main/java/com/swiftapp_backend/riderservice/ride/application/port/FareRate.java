package com.swiftapp_backend.riderservice.ride.application.port;

import java.math.BigDecimal;

/**
 * Effective fare rates for a ride type: per-type overrides merged over the
 * default rates.
 */
public record FareRate(
        BigDecimal baseFare,
        BigDecimal perKilometerRate,
        BigDecimal perMinuteRate
) {
}
