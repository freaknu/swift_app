package com.swiftapp_backend.riderservice.ride.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideRequestDto;
import com.swiftapp_backend.riderservice.ride.domain.enums.DiscountType;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.OfferEntity;
import com.swiftapp_backend.riderservice.shared.ApiException;
import com.swiftapp_backend.riderservice.shared.GlobalExceptionHandler;
import org.springframework.mock.web.MockHttpServletRequest;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Run all eligibility, lifecycle, rollback and concurrent quota tests on a disposable PostgreSQL DB. */
@EnabledIfSystemProperty(named = "swift.qa.postgres.url", matches = "jdbc:postgresql:.*")
@Import(RideOfferIntegrationTest.StubConfiguration.class)
class RideOfferPostgresTest extends RideOfferIntegrationTest {
    @DynamicPropertySource
    static void disposablePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("swift.qa.postgres.url"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> System.getProperty("swift.qa.postgres.username"));
        registry.add("spring.datasource.password", () -> System.getProperty("swift.qa.postgres.password"));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void contendedOfferLockReturnsRetryableJsonErrorWithinThreeSecondsAndConsumesNothing() throws Exception {
        var tx = new TransactionTemplate(transactionManager);
        Long offerId = tx.execute(status -> offerRows.saveAndFlush(OfferEntity.builder()
                .offerCode("LOCK-" + UUID.randomUUID()).title("Lock contention test")
                .discountType(DiscountType.PERCENTAGE).discountValue(BigDecimal.TEN).build()).getId());
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var holder = executor.submit(() -> tx.executeWithoutResult(status -> {
                offerRows.findByIdForUpdate(offerId).orElseThrow();
                locked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Holder timed out");
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
            }));
            try {
                assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
                long started = System.nanoTime();
                ApiException failure = catchThrowableOfType(ApiException.class, () -> rides.requestRide(
                        new RideRequestDto(10999L, offerId, 9999L, "BIKE",
                                new FareEstimateRequest(offerId, 12.9716, 77.5946, 12.9352, 77.6245, "BIKE"))));
                long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
                assertThat(failure).isNotNull();
                assertThat(failure.getStatus().value()).isEqualTo(503);
                assertThat(elapsedMillis).isBetween(2500L, 6500L);
                var response = new GlobalExceptionHandler().handleApiException(failure,
                        new MockHttpServletRequest("POST", "/api/v1/ride/requestRide"));
                assertThat(response.getStatusCode().value()).isEqualTo(503);
                assertThat(response.getBody().isSuccess()).isFalse();
                assertThat(response.getBody().getMessage()).contains("retry");
                assertThat(offerRows.findById(offerId).orElseThrow().getUsedCount()).isZero();
                assertThat(rideRows.countOfferUses(offerId, null)).isZero();
            } finally {
                release.countDown();
                holder.get(5, TimeUnit.SECONDS);
            }
        } finally { tx.executeWithoutResult(status -> offerRows.deleteById(offerId)); }
    }

}
