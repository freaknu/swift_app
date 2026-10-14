package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.domain.enums.VehicleTypeCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideTypesServiceTest {
    private final RideTypesService service = new RideTypesService();

    @Test
    void riderCatalogAlwaysContainsAllDriverVehicleTypesWithoutSeedRows() {
        List<String> expected = List.of("BIKE", "SCOOTER", "AUTO_RICKSHAW", "HATCHBACK",
                "SEDAN", "SUV", "LUXURY", "VAN", "ELECTRIC_BIKE", "ELECTRIC_CAR",
                "CARPOOL", "PREMIUM", "PREMIUM_SUV", "DELIVERY_BIKE", "DELIVERY_VAN");

        var types = service.getRideTypes(101L, false);
        assertThat(types).extracting("code").containsExactlyElementsOf(expected);
        assertThat(types).extracting("id").doesNotHaveDuplicates();
        assertThat(types).allSatisfy(type -> {
            assertThat(type.isActive()).isTrue();
            assertThat(type.getName()).isNotBlank();
            assertThat(type.getRideType()).isNotNull();
        });
        assertThat(service.getRideTypes(101L, true)).extracting("code")
                .containsExactlyElementsOf(expected);
    }

    @Test
    void canonicalAndLegacyCodesResolveWithoutGuessingUnknownTypes() {
        assertThat(VehicleTypeCode.fromRequest("HATCHBACK")).isEqualTo(VehicleTypeCode.HATCHBACK);
        assertThat(VehicleTypeCode.fromRequest("AUTO")).isEqualTo(VehicleTypeCode.AUTO_RICKSHAW);
        assertThat(VehicleTypeCode.fromRequest("STANDARD")).isEqualTo(VehicleTypeCode.SEDAN);
        assertThatThrownBy(() -> VehicleTypeCode.fromRequest("SPACESHIP"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
