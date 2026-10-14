package com.swiftapp_backend.locationservice.eta.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteCoordinatesTest {
    @Test
    void rejectsInvalidCoordinates() {
        assertThatThrownBy(() -> new RouteCoordinates(91, 77, 28, 77))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origin latitude");
    }
}
