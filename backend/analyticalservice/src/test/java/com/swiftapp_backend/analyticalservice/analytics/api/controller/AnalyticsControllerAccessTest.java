package com.swiftapp_backend.analyticalservice.analytics.api.controller;

import com.swiftapp_backend.analyticalservice.analytics.api.dto.DriverAnalyticsResponseDto;
import com.swiftapp_backend.analyticalservice.analytics.api.dto.RiderAnalyticsResponseDto;
import com.swiftapp_backend.analyticalservice.analytics.application.port.AnalyticsProfilePort;
import com.swiftapp_backend.analyticalservice.analytics.application.service.AnalyticsQueryService;
import com.swiftapp_backend.analyticalservice.shared.AnalyticsAccessDeniedException;
import com.swiftapp_backend.analyticalservice.shared.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyticsControllerAccessTest {
    private final StubQueries queries = new StubQueries();
    private final AnalyticsProfilePort profiles = new AnalyticsProfilePort() {
        @Override public Long riderAuthUserId(Long riderId) { return Long.valueOf(31).equals(riderId) ? 7L : null; }
        @Override public Long driverAuthUserId(Long driverId) { return Long.valueOf(42).equals(driverId) ? 8L : null; }
    };
    private final AnalyticsController controller = new AnalyticsController(queries, profiles);

    @Test
    void personalAnalyticsAndEarningsRequireProfileOwnership() {
        assertThrows(AnalyticsAccessDeniedException.class,
                () -> controller.getDriverAnalytics(7L, 42L));
        assertThrows(AnalyticsAccessDeniedException.class,
                () -> controller.getRiderAnalytics(8L, 31L));
        assertThrows(AnalyticsAccessDeniedException.class,
                () -> controller.getDriverEarnings(7L, 42L, "week"));
        assertEquals(0, queries.readCalls);

        assertEquals(200, controller.getDriverAnalytics(8L, 42L).getStatusCode().value());
        assertEquals(200, controller.getRiderAnalytics(7L, 31L).getStatusCode().value());
        assertEquals(200, controller.getDriverEarnings(8L, 42L, "week").getStatusCode().value());
        assertEquals(3, queries.readCalls);
    }

    @Test
    void globalDashboardIsUnavailableWithoutAdminIdentity() {
        AnalyticsAccessDeniedException denied = assertThrows(AnalyticsAccessDeniedException.class,
                () -> controller.getDashboardMetrics(8L));
        assertEquals(0, queries.readCalls);
        assertEquals(403, new GlobalExceptionHandler().handleAccessDenied(denied,
                new MockHttpServletRequest("GET", "/api/v1/analytics/dashboard"))
                .getStatusCode().value());
    }

    private static final class StubQueries extends AnalyticsQueryService {
        private int readCalls;

        private StubQueries() {
            super(null, null);
        }

        @Override public DriverAnalyticsResponseDto getDriverAnalytics(Long driverId) {
            readCalls++;
            return DriverAnalyticsResponseDto.builder().driverId(driverId).build();
        }

        @Override public RiderAnalyticsResponseDto getRiderAnalytics(Long riderId) {
            readCalls++;
            return RiderAnalyticsResponseDto.builder().riderId(riderId).build();
        }

        @Override public Double getDriverEarnings(Long driverId, String period) {
            readCalls++;
            return 12.0;
        }
    }
}
