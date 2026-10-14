package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateDto;
import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateRequest;
import com.swiftapp_backend.riderservice.ride.infrastructure.config.FareProperties;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FareServiceTest {

    /** Fixed 10 km / 1800 s route, no mocking framework needed. */
    private static class FixedDistanceUtil extends DistanceCalculateUtil {
        @Override
        public double calculateDistance(double startLatitude, double startLongitude,
                                        double endLatitude, double endLongitude) {
            return 10.0;
        }

        @Override
        public Long distanceCoverDuration(double startLatitude, double startLongitude,
                                          double endLatitude, double endLongitude, Long speed) {
            return 1800L;
        }
    }

    private FareService serviceWith(FareProperties props) {
        // estimate() never touches the repository or mapper.
        return new FareService(null, null, new FixedDistanceUtil(), props, null);
    }

    private FareProperties bikeOverrideProps() {
        FareProperties props = new FareProperties();
        FareProperties.TypeFare bike = new FareProperties.TypeFare();
        bike.setBaseFare(new BigDecimal("25.00"));
        bike.setPerKilometerRate(new BigDecimal("8.00"));
        bike.setPerMinuteRate(new BigDecimal("1.00"));
        props.getTypes().put("BIKE", bike);
        return props;
    }

    private FareEstimateRequest request(String rideType) {
        return new FareEstimateRequest(null, 28.6139, 77.2090, 28.7041, 77.1025, rideType);
    }

    @Test
    void usesPerTypeRatesWhenRideTypeMatches() {
        FareService service = serviceWith(bikeOverrideProps());

        FareEstimateDto dto = service.estimate(request("BIKE"));

        // 10 km * 8.00 = 80.00 distance; 30 min * 1.00 = 30.00 time; base 25.00
        assertThat(dto.fare().getDistanceFare()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(dto.fare().getTimeFare()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(dto.fare().getBaseFare()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void rejectsUnknownVehicleTypeInsteadOfQuotingTheWrongFare() {
        FareService service = serviceWith(bikeOverrideProps());

        assertThatThrownBy(() -> service.estimate(request("SPACESHIP")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported vehicle type");
    }

    @Test
    void fallsBackToDefaultRatesWhenRideTypeIsNull() {
        FareService service = serviceWith(bikeOverrideProps());

        FareEstimateDto dto = service.estimate(request(null));

        assertThat(dto.fare().getDistanceFare()).isEqualByComparingTo(new BigDecimal("120.00"));
    }

    @Test
    void bikeFareIsCheaperThanDefaultForSameRoute() {
        FareService service = serviceWith(bikeOverrideProps());

        BigDecimal bikeTotal = service.estimate(request("BIKE")).fare().getTotalFare();
        BigDecimal defaultTotal = service.estimate(request(null)).fare().getTotalFare();

        assertThat(bikeTotal).isLessThan(defaultTotal);
    }

    @Test
    void resolveRateIsCaseInsensitive() {
        FareProperties props = bikeOverrideProps();

        assertThat(props.resolveRate("bike").perKilometerRate())
                .isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(props.resolveRate("  BIKE ").perKilometerRate())
                .isEqualByComparingTo(new BigDecimal("8.00"));
    }

    @Test
    void canonicalBikeAndAutoCodesUseTheirConfiguredFareClasses() {
        FareProperties props = bikeOverrideProps();
        FareProperties.TypeFare auto = new FareProperties.TypeFare();
        auto.setBaseFare(new BigDecimal("30.00"));
        props.getTypes().put("AUTO", auto);

        FareService service = serviceWith(props);
        assertThat(service.estimate(request("ELECTRIC_BIKE")).fare().getBaseFare())
                .isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(service.estimate(request("AUTO_RICKSHAW")).fare().getBaseFare())
                .isEqualByComparingTo(new BigDecimal("30.00"));
    }
}
