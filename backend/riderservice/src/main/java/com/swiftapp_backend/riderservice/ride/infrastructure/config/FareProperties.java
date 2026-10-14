package com.swiftapp_backend.riderservice.ride.infrastructure.config;

import com.swiftapp_backend.riderservice.ride.application.port.FareConfigurationPort;
import com.swiftapp_backend.riderservice.ride.application.port.FareRate;
import com.swiftapp_backend.riderservice.ride.domain.enums.VehicleTypeCode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.fare")
public class FareProperties implements FareConfigurationPort {
    private BigDecimal baseFare = new BigDecimal("40.00");
    private BigDecimal perKilometerRate = new BigDecimal("12.00");
    private BigDecimal perMinuteRate = new BigDecimal("1.50");
    private BigDecimal bookingFee = new BigDecimal("10.00");
    private BigDecimal taxRate = new BigDecimal("0.05");
    private BigDecimal minimumFare = new BigDecimal("80.00");
    private BigDecimal surgeMultiplier = BigDecimal.ONE;
    private long averageSpeedKph = 30;

    /**
     * Per-ride-type rate overrides, keyed by upper-case ride type code
     * (e.g. BIKE, AUTO). Any rate left unset falls back to the default above.
     */
    private Map<String, TypeFare> types = new HashMap<>();

    @Data
    public static class TypeFare {
        private BigDecimal baseFare;
        private BigDecimal perKilometerRate;
        private BigDecimal perMinuteRate;
    }

    @Override
    public FareRate resolveRate(String rideType) {
        if (rideType != null) {
            TypeFare override = types.get(VehicleTypeCode.fromRequest(rideType).fareCode());
            if (override != null) {
                return new FareRate(
                        override.getBaseFare() != null ? override.getBaseFare() : getBaseFare(),
                        override.getPerKilometerRate() != null ? override.getPerKilometerRate() : getPerKilometerRate(),
                        override.getPerMinuteRate() != null ? override.getPerMinuteRate() : getPerMinuteRate());
            }
        }
        return new FareRate(getBaseFare(), getPerKilometerRate(), getPerMinuteRate());
    }
}
