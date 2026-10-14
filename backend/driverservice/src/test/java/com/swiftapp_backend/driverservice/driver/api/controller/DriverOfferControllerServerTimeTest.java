package com.swiftapp_backend.driverservice.driver.api.controller;

import com.swiftapp_backend.driverservice.driver.api.dto.PendingOfferDto;
import com.swiftapp_backend.driverservice.driver.application.service.DriverOfferService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DriverOfferControllerServerTimeTest {

    @Test
    void pendingOfferResponseUsesFreshServerTime() {
        DriverOfferService service = mock(DriverOfferService.class);
        PendingOfferDto offer = PendingOfferDto.builder()
                .rideId(101L)
                .expiresAt(Instant.now().plusSeconds(15).toString())
                .serverTime("2000-01-01T00:00:00Z")
                .build();
        when(service.getPendingOffers(7L)).thenReturn(List.of(offer));
        DriverOfferController controller = new DriverOfferController(service);
        Instant before = Instant.now();

        var response = controller.pendingOffers(7L);

        Instant after = Instant.now();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        Instant serverTime = Instant.parse(response.getBody().getData().get(0).getServerTime());
        assertThat(serverTime).isBetween(before, after);
        assertThat(response.getBody().getData().get(0).getExpiresAt())
                .isEqualTo(offer.getExpiresAt());
    }
}
