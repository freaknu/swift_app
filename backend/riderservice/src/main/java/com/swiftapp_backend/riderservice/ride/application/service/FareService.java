package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.application.port.FareConfigurationPort;
import com.swiftapp_backend.riderservice.ride.application.port.FareRate;
import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareDto;
import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateDto;
import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateRequest;
import com.swiftapp_backend.riderservice.ride.domain.enums.Currency;
import com.swiftapp_backend.riderservice.ride.domain.enums.PaymentStatus;
import com.swiftapp_backend.riderservice.ride.domain.enums.VehicleTypeCode;
import com.swiftapp_backend.riderservice.ride.application.mapper.FareMapper;
import com.swiftapp_backend.riderservice.ride.domain.model.FareDetailModel;
import com.swiftapp_backend.riderservice.ride.domain.model.OfferModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.FareDetailRepository;
import com.swiftapp_backend.riderservice.shared.ApiException;
import com.swiftapp_backend.riderservice.ride.application.service.DistanceCalculateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FareService {
    private static final int MONEY_SCALE = 2;

    private final FareDetailRepository fareDetailRepository;
    private final FareMapper fareMapper;
    private final DistanceCalculateUtil distanceCalculateUtil;
    private final FareConfigurationPort fareProperties;
    private final OfferService offerService;

    public record BookingFare(FareEstimateDto estimate, OfferModel offer) {}

    public FareEstimateDto estimate(FareEstimateRequest route) {
        return calculate(route, null, false).estimate();
    }

    public FareEstimateDto estimate(FareEstimateRequest route, Long authUserId) {
        Long riderId = route != null && route.offerId() != null
                ? offerService.riderIdForAuthUser(authUserId) : null;
        return calculate(route, riderId, false).estimate();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BookingFare estimateForBooking(FareEstimateRequest route, Long riderId) {
        return calculate(route, riderId, true);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseOffer(OfferModel offer) {
        if (offer != null) offerService.release(offer.getId());
    }

    private BookingFare calculate(FareEstimateRequest route, Long riderId, boolean booking) {
        validateRoute(route);

        double distance = distanceCalculateUtil.calculateDistance(
                route.pickupLatitude(), route.pickupLongitude(),
                route.destinationLatitude(), route.destinationLongitude());
        if (distance < 0.05) {
            throw ApiException.badRequest("Pickup and destination must be at least 50 metres apart");
        }

        long duration = distanceCalculateUtil.distanceCoverDuration(
                route.pickupLatitude(), route.pickupLongitude(),
                route.destinationLatitude(), route.destinationLongitude(),
                fareProperties.getAverageSpeedKph());

        FareRate rate = fareProperties.resolveRate(route.rideType());
        BigDecimal distanceFare = money(BigDecimal.valueOf(distance)
                .multiply(rate.perKilometerRate()));
        BigDecimal minutes = BigDecimal.valueOf(duration)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal timeFare = money(minutes.multiply(rate.perMinuteRate()));
        BigDecimal meteredFare = rate.baseFare().add(distanceFare).add(timeFare);
        BigDecimal surgedFare = meteredFare.multiply(fareProperties.getSurgeMultiplier());
        BigDecimal preTaxTotal = surgedFare.add(fareProperties.getBookingFee())
                .max(fareProperties.getMinimumFare());
        BigDecimal tax = money(preTaxTotal.multiply(fareProperties.getTaxRate()));
        BigDecimal grossFare = money(preTaxTotal.add(tax));
        OfferService.Discount applied = route.offerId() == null ? null
                : booking ? offerService.reserve(route.offerId(), route.rideType(), grossFare, riderId)
                : offerService.quote(route.offerId(), route.rideType(), grossFare, riderId);
        BigDecimal discount = applied == null ? money(BigDecimal.ZERO) : applied.amount();
        BigDecimal total = money(grossFare.subtract(discount));

        FareDto fare = FareDto.builder()
                .baseFare(money(rate.baseFare()))
                .distanceFare(distanceFare)
                .timeFare(timeFare)
                .bookingFee(money(fareProperties.getBookingFee()))
                .tax(tax)
                .surgeMultiplier(fareProperties.getSurgeMultiplier())
                .discount(discount)
                .totalFare(total)
                .currency(Currency.INR)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        FareEstimateDto estimate = FareEstimateDto.builder()
                .estimatedDistanceKm(Math.round(distance * 100.0) / 100.0)
                .estimatedDurationSeconds(duration)
                .fare(fare)
                .build();
        return new BookingFare(estimate, applied == null ? null : applied.offer());
    }

    @Transactional(readOnly = true)
    public FareDto getFareByRideId(Long rideId) {
        FareDetailModel fare = fareDetailRepository.findByRideId(rideId)
                .orElseThrow(() -> ApiException.notFound("Fare not found for ride " + rideId));
        return fareMapper.toDto(fare);
    }

    private void validateRoute(FareEstimateRequest route) {
        if (route == null || route.pickupLatitude() == null || route.pickupLongitude() == null
                || route.destinationLatitude() == null || route.destinationLongitude() == null) {
            throw ApiException.badRequest("Pickup and destination coordinates are required");
        }
        if (Math.abs(route.pickupLatitude()) > 90 || Math.abs(route.destinationLatitude()) > 90
                || Math.abs(route.pickupLongitude()) > 180 || Math.abs(route.destinationLongitude()) > 180) {
            throw ApiException.badRequest("Coordinates are outside the valid latitude/longitude range");
        }
        if (route.offerId() != null && route.offerId() <= 0) {
            throw ApiException.badRequest("Offer ID must be positive");
        }
        if (!Double.isFinite(route.pickupLatitude()) || !Double.isFinite(route.pickupLongitude())
                || !Double.isFinite(route.destinationLatitude()) || !Double.isFinite(route.destinationLongitude())) {
            throw ApiException.badRequest("Coordinates must be finite numbers");
        }
        if (route.rideType() != null) {
            try {
                VehicleTypeCode.fromRequest(route.rideType());
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("Unsupported vehicle type: " + route.rideType());
            }
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
