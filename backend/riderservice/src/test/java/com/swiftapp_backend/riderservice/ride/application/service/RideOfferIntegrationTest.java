package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.offer.OfferFetchRequestDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.AcceptRideRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.CancelRideRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideRequestDto;
import com.swiftapp_backend.riderservice.ride.application.port.MatchingCancellationPort;
import com.swiftapp_backend.riderservice.ride.application.port.MessagePublisherPort;
import com.swiftapp_backend.riderservice.ride.application.port.RideIdentityPort;
import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import com.swiftapp_backend.riderservice.ride.domain.enums.DiscountType;
import com.swiftapp_backend.riderservice.ride.domain.event.RideCompletedEventDto;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.OfferEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaOfferRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideRepository;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:rideoffers;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@Transactional
class RideOfferIntegrationTest {
    @TestConfiguration
    static class StubConfiguration {
        @Bean @Primary MatchingCancellationPort offerMatching() {
            return rideId -> MatchingCancellationPort.Result.CANCELLED;
        }
        @Bean @Primary RideIdentityPort offerIdentities() {
            return new RideIdentityPort() {
                @Override public Long riderIdForAuthUser(Long authUserId) { return authUserId - 1000; }
                @Override public Long driverIdForAuthUser(Long authUserId) { return authUserId - 2000; }
                @Override public Long riderAuthUserId(Long riderId) { return riderId + 1000; }
                @Override public Long driverAuthUserId(Long driverId) { return driverId + 2000; }
            };
        }
        @Bean @Primary CoordinatedDistance coordinatedDistance() { return new CoordinatedDistance(); }
        @Bean @Primary RecordingPublisher offerPublisher() { return new RecordingPublisher(); }
    }
    static class CoordinatedDistance extends DistanceCalculateUtil {
        volatile CountDownLatch bookingBarrier;
        @Override public double calculateDistance(double a, double b, double c, double d) {
            CountDownLatch barrier = bookingBarrier;
            if (barrier != null) {
                barrier.countDown();
                try {
                    if (!barrier.await(5, TimeUnit.SECONDS)) throw new AssertionError("Booking barrier timed out");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
            }
            return super.calculateDistance(a, b, c, d);
        }
    }
    static class RecordingPublisher implements MessagePublisherPort {
        final ConcurrentMap<Long, RideCompletedEventDto> completed = new ConcurrentHashMap<>();
        @Override public void publish(String topic, String key, Object event) {
            if ("ride.completed".equals(topic) && event instanceof RideCompletedEventDto dto) {
                completed.put(dto.getRideId(), dto);
            }
        }
    }

    @Autowired FareService fares;
    @Autowired OfferService offers;
    @Autowired RideService rides;
    @Autowired JpaOfferRepository offerRows;
    @Autowired JpaRideRepository rideRows;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired RecordingPublisher publisher;
    @Autowired CoordinatedDistance distance;
    @Autowired JdbcTemplate jdbc;

    private FareEstimateRequest route(Long offerId, String type) {
        return new FareEstimateRequest(offerId, 12.9716, 77.5946, 12.9352, 77.6245, type);
    }
    private OfferEntity promotion(DiscountType type, String value) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        return offerRows.saveAndFlush(OfferEntity.builder().offerCode("TEST-" + UUID.randomUUID())
                .title("Integration test offer").discountType(type).discountValue(new BigDecimal(value))
                .validFrom(now.minusHours(1)).validUntil(now.plusDays(1))
                .active(true).usageLimit(100).perUserLimit(3).build());
    }
    private com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto book(Long offerId, long riderId) {
        return rides.requestRide(new RideRequestDto(riderId + 1000, offerId, riderId,
                "BIKE", route(offerId, "BIKE")));
    }

    @Test void percentageUsesGrossFareAndRoundsCurrencyWithMaximumCap() {
        var offer = promotion(DiscountType.PERCENTAGE, "12.5");
        BigDecimal gross = fares.estimate(route(null, "BIKE")).fare().getTotalFare();
        var discounted = fares.estimate(route(offer.getId(), "BIKE"), 1001L).fare();
        BigDecimal expected = gross.multiply(new BigDecimal("0.125")).setScale(2, RoundingMode.HALF_UP);
        assertThat(discounted.getDiscount()).isEqualByComparingTo(expected);
        assertThat(discounted.getTotalFare()).isEqualByComparingTo(gross.subtract(expected));
        offer.setMaximumDiscount(new BigDecimal("5.00"));
        offerRows.saveAndFlush(offer);
        assertThat(fares.estimate(route(offer.getId(), "BIKE"), 1001L).fare().getDiscount())
                .isEqualByComparingTo("5.00");
    }

    @Test void fixedAmountHonoursCapAndMinimumFare() {
        var offer = promotion(DiscountType.FIXED_AMOUNT, "20");
        offer.setMaximumDiscount(new BigDecimal("12"));
        offerRows.saveAndFlush(offer);
        assertThat(fares.estimate(route(offer.getId(), "BIKE"), 1001L).fare().getDiscount())
                .isEqualByComparingTo("12.00");
        offer.setMinimumFare(new BigDecimal("100000"));
        offerRows.saveAndFlush(offer);
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE"), 1001L))
                .isInstanceOf(ApiException.class).hasMessageContaining("at least");
    }

    @Test void fixedAndPercentageDiscountsRetainOneRupeePayable() {
        var fixed = promotion(DiscountType.FIXED_AMOUNT, "100000");
        var percent = promotion(DiscountType.PERCENTAGE, "100");
        for (var offer : java.util.List.of(fixed, percent)) {
            var fare = fares.estimate(route(offer.getId(), "BIKE"), 1001L).fare();
            assertThat(fare.getTotalFare()).isEqualByComparingTo("1.00");
            assertThat(fare.getDiscount()).isPositive();
        }
    }

    @Test void expiredFutureInactiveInvalidAndWrongVehicleOffersAreRejected() {
        var offer = promotion(DiscountType.PERCENTAGE, "10");
        offer.setValidUntil(LocalDateTime.now(Clock.systemUTC()).minusMinutes(1));
        offerRows.saveAndFlush(offer);
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE"), 1001L))
                .isInstanceOf(ApiException.class).hasMessageContaining("expired");
        offer.setValidUntil(LocalDateTime.now(Clock.systemUTC()).plusDays(1));
        offer.setValidFrom(LocalDateTime.now(Clock.systemUTC()).plusHours(1));
        offerRows.saveAndFlush(offer);
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE"), 1001L))
                .isInstanceOf(ApiException.class).hasMessageContaining("not active yet");
        offer.setValidFrom(null);
        offer.setActive(false);
        offerRows.saveAndFlush(offer);
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE"), 1001L))
                .isInstanceOf(ApiException.class);
        offer.setActive(true);
        offer.setDiscountValue(new BigDecimal("101"));
        offerRows.saveAndFlush(offer);
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE"), 1001L))
                .isInstanceOf(ApiException.class).hasMessageContaining("not available");
        offer.setDiscountValue(BigDecimal.TEN);
        offer.setRideTypeCode("SEDAN");
        offerRows.saveAndFlush(offer);
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE"), 1001L))
                .isInstanceOf(ApiException.class).hasMessageContaining("vehicle type");
    }

    @Test void unavailableOfferAndUnauthenticatedQuoteCannotSilentlyChargeFullFare() {
        assertThatThrownBy(() -> fares.estimate(route(Long.MAX_VALUE, "BIKE"), 1001L))
                .isInstanceOf(ApiException.class).hasMessageContaining("not available");
        var offer = promotion(DiscountType.PERCENTAGE, "10");
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE")))
                .isInstanceOf(ApiException.class).hasMessageContaining("Authenticated rider");
    }

    @Test void listingIncludesGenericAndRequestedVehicleOnlyAndFiltersEligibility() {
        var generic = promotion(DiscountType.PERCENTAGE, "10");
        var bike = promotion(DiscountType.FIXED_AMOUNT, "10");
        bike.setRideTypeCode("BIKE");
        offerRows.saveAndFlush(bike);
        var car = promotion(DiscountType.FIXED_AMOUNT, "10");
        car.setRideTypeCode("SEDAN");
        offerRows.saveAndFlush(car);
        var expired = promotion(DiscountType.PERCENTAGE, "10");
        expired.setValidUntil(LocalDateTime.now(Clock.systemUTC()).minusHours(1));
        offerRows.saveAndFlush(expired);
        var exhausted = promotion(DiscountType.PERCENTAGE, "10");
        exhausted.setUsageLimit(0);
        offerRows.saveAndFlush(exhausted);
        assertThat(offers.getAllOffers(1001L, new OfferFetchRequestDto("BIKE", null)))
                .extracting("id").containsExactlyInAnyOrder(generic.getId(), bike.getId());
        assertThat(offers.getAllOffers(1001L, new OfferFetchRequestDto(null, null)))
                .extracting("id").containsExactlyInAnyOrder(generic.getId(), bike.getId(), car.getId());
    }

    @Test void inactiveOffersStayInactiveWhenFirstPersisted() {
        var offer = OfferEntity.builder().offerCode("INACTIVE-" + UUID.randomUUID()).title("Inactive")
                .discountType(DiscountType.FIXED_AMOUNT).discountValue(BigDecimal.TEN).active(false).build();
        offer = offerRows.saveAndFlush(offer);
        assertThat(offerRows.findById(offer.getId()).orElseThrow().isActive()).isFalse();
    }

    @Test void repeatedQuotesConsumeNothingAndOneOfferCanBeUsedByManyRiders() {
        var offer = promotion(DiscountType.PERCENTAGE, "10");
        for (int i = 0; i < 3; i++) fares.estimate(route(offer.getId(), "BIKE"), 1001L);
        assertThat(offerRows.findById(offer.getId()).orElseThrow().getUsedCount()).isZero();
        var first = book(offer.getId(), 1);
        var second = book(offer.getId(), 2);
        offerRows.flush();
        assertThat(first.getFareDetail().getDiscount()).isPositive();
        assertThat(second.getFareDetail().getTotalFare()).isEqualByComparingTo(first.getFareDetail().getTotalFare());
        assertThat(rideRows.findById(first.getId()).orElseThrow().getOffer().getId()).isEqualTo(offer.getId());
        assertThat(rideRows.findById(second.getId()).orElseThrow().getOffer().getId()).isEqualTo(offer.getId());
        assertThat(offerRows.findById(offer.getId()).orElseThrow().getUsedCount()).isEqualTo(2);
    }

    @Test void rootAndNestedOfferMustMatchAndEitherLegacyLocationIsAccepted() {
        var one = promotion(DiscountType.PERCENTAGE, "10");
        var two = promotion(DiscountType.PERCENTAGE, "20");
        assertThatThrownBy(() -> rides.requestRide(new RideRequestDto(1001L, one.getId(), 1L,
                "BIKE", route(two.getId(), "BIKE"))))
                .isInstanceOf(ApiException.class).hasMessageContaining("offerId must match");
        var rootOnly = rides.requestRide(new RideRequestDto(1001L, one.getId(), 1L,
                "BIKE", route(null, "BIKE")));
        var nestedOnly = rides.requestRide(new RideRequestDto(1002L, null, 2L,
                "BIKE", route(one.getId(), "BIKE")));
        assertThat(rootOnly.getFareDetail().getDiscount()).isPositive();
        assertThat(nestedOnly.getFareDetail().getDiscount()).isPositive();
    }

    @Test void completionCarriesExactlyTheDiscountedPersistedFareAndRetainsUsage() {
        var offer = promotion(DiscountType.FIXED_AMOUNT, "20");
        var booked = book(offer.getId(), 1);
        rides.acceptRide(booked.getId(), new AcceptRideRequest(50L, 70L));
        rides.driverArrived(booked.getId(), 50L);
        rides.startRide(booked.getId(), 50L);
        var completed = rides.completeRide(booked.getId(), 50L);
        assertThat(completed.getFareDetail().getDiscount()).isEqualByComparingTo("20.00");
        assertThat(publisher.completed.get(booked.getId()).getTotalFare())
                .isEqualByComparingTo(completed.getFareDetail().getTotalFare());
        assertThat(fares.getFareByRideId(booked.getId()).getTotalFare())
                .isEqualByComparingTo(completed.getFareDetail().getTotalFare());
        assertThat(offerRows.findById(offer.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
    }

    @Test void matchingFailureAndCancellationReleaseQuotaButCompletedRideConsumesIt() {
        var offer = promotion(DiscountType.FIXED_AMOUNT, "20");
        offer.setPerUserLimit(1);
        offerRows.saveAndFlush(offer);
        var first = book(offer.getId(), 1);
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE"), 1001L))
                .isInstanceOf(ApiException.class).hasMessageContaining("maximum number");
        rides.failMatching(first.getId(), 1L, "NO_AVAILABLE_DRIVER");
        assertThat(offerRows.findById(offer.getId()).orElseThrow().getUsedCount()).isZero();
        var retry = book(offer.getId(), 1);
        rides.cancelRide(retry.getId(), new CancelRideRequest(1L, ChangedByType.RIDER, "Test retry"));
        assertThat(offerRows.findById(offer.getId()).orElseThrow().getUsedCount()).isZero();
        var completed = book(offer.getId(), 1);
        rides.acceptRide(completed.getId(), new AcceptRideRequest(50L, 70L));
        rides.driverArrived(completed.getId(), 50L);
        rides.startRide(completed.getId(), 50L);
        rides.completeRide(completed.getId(), 50L);
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE"), 1001L))
                .isInstanceOf(ApiException.class).hasMessageContaining("maximum number");
    }

    @Test void newRiderOnlyIsEnforcedAgainstCompletedRidesAndAuthenticatedProfile() {
        var offer = promotion(DiscountType.FIXED_AMOUNT, "20");
        offer.setNewUserOnly(true);
        offerRows.saveAndFlush(offer);
        var first = book(offer.getId(), 1);
        rides.acceptRide(first.getId(), new AcceptRideRequest(50L, 70L));
        rides.driverArrived(first.getId(), 50L);
        rides.startRide(first.getId(), 50L);
        rides.completeRide(first.getId(), 50L);
        assertThatThrownBy(() -> fares.estimate(route(offer.getId(), "BIKE"), 1001L))
                .isInstanceOf(ApiException.class).hasMessageContaining("first completed ride");
        assertThat(fares.estimate(route(offer.getId(), "BIKE"), 1002L).fare().getDiscount()).isPositive();
        assertThat(offers.getAllOffers(1001L, new OfferFetchRequestDto("BIKE", null))).isEmpty();
    }

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedBookingRollsBackOfferReservationAndRideTogether() {
        var tx = new TransactionTemplate(transactionManager);
        Long offerId = tx.execute(status -> promotion(DiscountType.PERCENTAGE, "10").getId());
        try {
            assertThatThrownBy(() -> tx.execute(status -> {
                book(offerId, 9001);
                throw new IllegalStateException("Simulated failure after ride persistence");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(offerRows.findById(offerId).orElseThrow().getUsedCount()).isZero();
            assertThat(rideRows.countOfferUses(offerId, null)).isZero();
        } finally { tx.executeWithoutResult(status -> offerRows.deleteById(offerId)); }
    }

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentBookingsCannotExceedGlobalQuota() throws Exception {
        var tx = new TransactionTemplate(transactionManager);
        Long offerId = tx.execute(status -> {
            var row = promotion(DiscountType.PERCENTAGE, "10");
            row.setUsageLimit(1);
            return offerRows.saveAndFlush(row).getId();
        });
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.List<java.util.concurrent.Future<Boolean>> attempts = new java.util.ArrayList<>();
            for (long rider : new long[] {9002, 9003}) {
                attempts.add(executor.submit(() -> {
                    start.await();
                    try { book(offerId, rider); return true; }
                    catch (ApiException exhausted) {
                        assertThat(exhausted.getMessage()).contains("usage limit");
                        return false;
                    }
                }));
            }
            start.countDown();
            int booked = 0;
            for (var attempt : attempts) if (attempt.get(10, TimeUnit.SECONDS)) booked++;
            assertThat(booked).isEqualTo(1);
            assertThat(rideRows.countOfferUses(offerId, null)).isEqualTo(1);
            assertThat(offerRows.findById(offerId).orElseThrow().getUsedCount()).isEqualTo(1);
        } finally {
            tx.executeWithoutResult(status -> {
                rideRows.findAll().stream().filter(r -> r.getOffer() != null && r.getOffer().getId().equals(offerId))
                        .forEach(rideRows::delete);
                rideRows.flush();
                offerRows.deleteById(offerId);
            });
        }
    }
    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentBookingsForSameRiderCannotExceedPerUserQuota() throws Exception {
        var tx = new TransactionTemplate(transactionManager);
        Long offerId = tx.execute(status -> {
            var row = promotion(DiscountType.PERCENTAGE, "10");
            row.setPerUserLimit(1);
            return offerRows.saveAndFlush(row).getId();
        });
        distance.bookingBarrier = new CountDownLatch(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.List<java.util.concurrent.Future<Boolean>> attempts = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                attempts.add(executor.submit(() -> {
                    try { book(offerId, 9004); return true; }
                    catch (ApiException exhausted) {
                        assertThat(exhausted.getMessage()).contains("maximum number");
                        return false;
                    }
                }));
            }
            int booked = 0;
            for (var attempt : attempts) if (attempt.get(10, TimeUnit.SECONDS)) booked++;
            assertThat(booked).isEqualTo(1);
            assertThat(rideRows.countOfferUses(offerId, 9004L)).isEqualTo(1);
            assertThat(offerRows.findById(offerId).orElseThrow().getUsedCount()).isEqualTo(1);
        } finally {
            distance.bookingBarrier = null;
            tx.executeWithoutResult(status -> {
                rideRows.findAll().stream().filter(r -> r.getOffer() != null && r.getOffer().getId().equals(offerId))
                        .forEach(rideRows::delete);
                rideRows.flush();
                offerRows.deleteById(offerId);
            });
        }
    }

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void releasingPreloadedOfferDoesNotOverwriteAnInterveningCampaignEdit() {
        var tx = new TransactionTemplate(transactionManager);
        Long offerId = tx.execute(status -> promotion(DiscountType.PERCENTAGE, "10").getId());
        var booked = book(offerId, 9005);
        try {
            tx.executeWithoutResult(status -> {
                // Keep the old campaign snapshot in this transaction's persistence context.
                assertThat(rideRows.findById(booked.getId()).orElseThrow().getOffer().getTitle())
                        .isEqualTo("Integration test offer");
                var editor = new TransactionTemplate(transactionManager);
                editor.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                editor.executeWithoutResult(edit -> jdbc.update(
                        "update offers set title = ?, discount_value = ?, is_active = false where id = ?",
                        "Campaign edited", new BigDecimal("25"), offerId));
                rides.cancelRide(booked.getId(), new CancelRideRequest(9005L, ChangedByType.RIDER, "Test edit"));
            });
            var after = offerRows.findById(offerId).orElseThrow();
            assertThat(after.getTitle()).isEqualTo("Campaign edited");
            assertThat(after.getDiscountValue()).isEqualByComparingTo("25");
            assertThat(after.isActive()).isFalse();
            assertThat(after.getUsedCount()).isZero();
        } finally {
            tx.executeWithoutResult(status -> {
                rideRows.deleteById(booked.getId());
                rideRows.flush();
                offerRows.deleteById(offerId);
            });
        }
    }

}
