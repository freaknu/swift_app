package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.api.dto.offer.OfferFetchRequestDto;
import com.swiftapp_backend.riderservice.ride.api.dto.offer.OfferResponseDto;
import com.swiftapp_backend.riderservice.ride.application.mapper.OfferMapper;
import com.swiftapp_backend.riderservice.ride.application.port.RideIdentityPort;
import com.swiftapp_backend.riderservice.ride.domain.enums.DiscountType;
import com.swiftapp_backend.riderservice.ride.domain.enums.VehicleTypeCode;
import com.swiftapp_backend.riderservice.ride.domain.model.OfferModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.OfferRepository;
import com.swiftapp_backend.riderservice.shared.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferService {
    private static final BigDecimal MINIMUM_PAYABLE = new BigDecimal("1.00");
    private final OfferRepository offerRepository;
    private final RideIdentityPort identityPort;

    public record Discount(OfferModel offer, BigDecimal amount) {}

    public Long riderIdForAuthUser(Long authUserId) {
        if (authUserId == null || authUserId <= 0) {
            throw ApiException.forbidden("Authenticated rider is required to apply an offer");
        }
        Long riderId = identityPort.riderIdForAuthUser(authUserId);
        if (riderId == null || riderId <= 0) {
            throw ApiException.forbidden("Authenticated rider profile was not found");
        }
        return riderId;
    }

    @Transactional(readOnly = true)
    public List<OfferResponseDto> getAllOffers(Long authUserId, OfferFetchRequestDto data) {
        if (data == null) throw ApiException.badRequest("Offer request is required");
        String rideType = data.getRideTypeCode() == null || data.getRideTypeCode().isBlank()
                ? null : data.getRideTypeCode();
        validateVehicle(rideType);
        Long riderId = riderIdForAuthUser(authUserId);
        List<OfferResponseDto> available = new ArrayList<>();
        for (OfferModel offer : offerRepository.findActive()) {
            try {
                validateEligibility(offer, rideType == null ? offer.getRideTypeCode() : rideType, riderId);
                offer.setUsedCount(Math.toIntExact(offerRepository.countUses(offer.getId(), null)));
                available.add(OfferMapper.toDto(offer));
            } catch (ApiException ignored) {
                // Lists contain current eligible offers; selected offers are revalidated at booking.
            }
        }
        return available;
    }

    @Transactional(readOnly = true)
    public Discount quote(Long offerId, String rideType, BigDecimal grossFare, Long riderId) {
        OfferModel offer = offerRepository.findById(offerId)
                .orElseThrow(() -> ApiException.badRequest("Selected offer is not available"));
        validateEligibility(offer, rideType, riderId);
        return new Discount(offer, calculateDiscount(offer, grossFare));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Discount reserve(Long offerId, String rideType, BigDecimal grossFare, Long riderId) {
        // The existing offer row serializes bookings, so concurrent riders cannot exceed its quota.
        OfferModel offer = offerRepository.findByIdForUpdate(offerId)
                .orElseThrow(() -> ApiException.badRequest("Selected offer is not available"));
        validateEligibility(offer, rideType, riderId);
        BigDecimal discount = calculateDiscount(offer, grossFare);
        offer.setUsedCount(Math.toIntExact(offerRepository.countUses(offerId, null) + 1));
        return new Discount(offerRepository.save(offer), discount);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Long offerId) {
        OfferModel offer = offerRepository.findByIdForUpdate(offerId)
                .orElseThrow(() -> ApiException.conflict("Applied offer was not found"));
        // The ride's terminal status has already been saved in the same transaction.
        offer.setUsedCount(Math.toIntExact(offerRepository.countUses(offerId, null)));
        offerRepository.save(offer);
    }

    private void validateEligibility(OfferModel offer, String rideType, Long riderId) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        if (!offer.isActive() || (offer.getValidFrom() != null && now.isBefore(offer.getValidFrom()))
                || (offer.getValidUntil() != null && !now.isBefore(offer.getValidUntil()))) {
            throw ApiException.badRequest("Selected offer has expired or is not active yet");
        }
        if (!hasValidTerms(offer)) throw ApiException.badRequest("Selected offer is not available");
        if (offer.getRideTypeCode() != null && !offer.getRideTypeCode().isBlank()) {
            try {
                if (rideType == null || VehicleTypeCode.fromRequest(offer.getRideTypeCode())
                        != VehicleTypeCode.fromRequest(rideType)) {
                    throw ApiException.badRequest("Selected offer does not apply to this vehicle type");
                }
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("Selected offer does not apply to this vehicle type");
            }
        }
        if (riderId == null || riderId <= 0) {
            throw ApiException.forbidden("Authenticated rider is required to apply an offer");
        }
        if (offer.getUsageLimit() != null
                && offerRepository.countUses(offer.getId(), null) >= offer.getUsageLimit()) {
            throw ApiException.badRequest("Selected offer has reached its usage limit");
        }
        if (offer.getPerUserLimit() != null
                && offerRepository.countUses(offer.getId(), riderId) >= offer.getPerUserLimit()) {
            throw ApiException.badRequest("You have already used this offer the maximum number of times");
        }
        if (offer.isNewUserOnly() && offerRepository.hasCompletedRide(riderId)) {
            throw ApiException.badRequest("Selected offer is only available for your first completed ride");
        }
    }

    private boolean hasValidTerms(OfferModel offer) {
        return offer.getDiscountType() != null && offer.getDiscountValue() != null
                && offer.getDiscountValue().signum() > 0
                && (offer.getDiscountType() != DiscountType.PERCENTAGE
                    || offer.getDiscountValue().compareTo(new BigDecimal("100")) <= 0)
                && (offer.getMaximumDiscount() == null || offer.getMaximumDiscount().signum() > 0)
                && (offer.getMinimumFare() == null || offer.getMinimumFare().signum() >= 0)
                && (offer.getUsageLimit() == null || offer.getUsageLimit() >= 0)
                && (offer.getPerUserLimit() == null || offer.getPerUserLimit() >= 0);
    }

    private BigDecimal calculateDiscount(OfferModel offer, BigDecimal grossFare) {
        if (offer.getMinimumFare() != null && grossFare.compareTo(offer.getMinimumFare()) < 0) {
            throw ApiException.badRequest("This offer needs a fare of at least ₹"
                    + offer.getMinimumFare().setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal discount = offer.getDiscountType() == DiscountType.PERCENTAGE
                ? grossFare.multiply(offer.getDiscountValue()).movePointLeft(2)
                : offer.getDiscountValue();
        if (offer.getMaximumDiscount() != null) discount = discount.min(offer.getMaximumDiscount());
        // This app collects through Razorpay; a free/zero-value order is not supported.
        discount = discount.min(grossFare.subtract(MINIMUM_PAYABLE)).setScale(2, RoundingMode.HALF_UP);
        if (discount.signum() <= 0) throw ApiException.badRequest("Selected offer cannot reduce this fare");
        return discount;
    }

    private void validateVehicle(String rideType) {
        if (rideType == null || rideType.isBlank()) return;
        try {
            VehicleTypeCode.fromRequest(rideType);
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unsupported vehicle type: " + rideType);
        }
    }
}
