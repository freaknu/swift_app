package com.swiftapp_backend.riderservice.ride.application.port;

import java.math.BigDecimal;

public interface FareConfigurationPort {
    BigDecimal getBaseFare();

    BigDecimal getPerKilometerRate();

    BigDecimal getPerMinuteRate();

    BigDecimal getBookingFee();

    BigDecimal getTaxRate();

    BigDecimal getMinimumFare();

    BigDecimal getSurgeMultiplier();

    long getAverageSpeedKph();

    /**
     * Effective rates for a ride type code (e.g. BIKE, AUTO, STANDARD).
     * Unknown or blank codes fall back to the default rates.
     */
    FareRate resolveRate(String rideType);
}
