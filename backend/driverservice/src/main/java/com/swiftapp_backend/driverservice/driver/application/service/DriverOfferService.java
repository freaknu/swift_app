package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.PendingOfferDto;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.MatchingDecisionPort;
import com.swiftapp_backend.driverservice.driver.application.port.RiderRideLookupPort;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Driver-side ride offers.
 *
 * <p>The matching service offers each ride to one driver at a time. The offer
 * waits in Redis until the driver decides or its deadline passes. Decisions
 * are confirmed synchronously by matching, which settles the current offer
 * atomically before this service reports success to the app.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverOfferService {

    private static final String PENDING_OFFER_KEY_PREFIX = "driver:pending-offer:";
    /** Redis retention exceeds the matching deadline; getPendingOffers still checks expiry. */
    private static final long PENDING_OFFER_TTL_SECONDS = 60;

    private static final String ACTIVE_RIDE_KEY_PREFIX = "driver:active-ride:";

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverCachePort cachePort;
    private final MatchingDecisionPort matchingClient;
    private final RiderRideLookupPort riderRideLookupClient;
    private final DriverRegistrationService registrationService;
    private final ObjectMapper objectMapper;

    /** Called by the Kafka consumer when {@code ride.offer.broadcast} arrives. */
    public void savePendingOffer(Long driverId, Long rideId, Long riderId,
                                 double distanceKm, String expiresAt) {
        savePendingOffer(driverId, rideId, riderId, distanceKm, expiresAt,
                null, null, null, null, null, null, null, null, null);
    }

    public void savePendingOffer(Long driverId, Long rideId, Long riderId,
                                 double distanceKm, String expiresAt,
                                 Double pickupLatitude, Double pickupLongitude,
                                 Double destinationLatitude, Double destinationLongitude,
                                 Double estimatedDistance, Long estimatedDuration,
                                 java.math.BigDecimal totalFare, String currency,
                                 String rideTypeCode) {
        PendingOfferDto offer = PendingOfferDto.builder()
                .rideId(rideId)
                .riderId(riderId)
                .driverId(driverId)
                .distanceKm(distanceKm)
                .pickupLatitude(pickupLatitude)
                .pickupLongitude(pickupLongitude)
                .destinationLatitude(destinationLatitude)
                .destinationLongitude(destinationLongitude)
                .estimatedDistance(estimatedDistance)
                .estimatedDuration(estimatedDuration)
                .totalFare(totalFare)
                .currency(currency)
                .rideTypeCode(rideTypeCode)
                .expiresAt(expiresAt)
                .build();
        cachePort.set(key(driverId), offer, PENDING_OFFER_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("Pending offer stored: ride={} driver={} expiresAt={}",
                rideId, driverId, expiresAt);
    }

    /** Offers currently waiting for this driver's decision. */
    public List<PendingOfferDto> getPendingOffers(Long authUserId) {
        Long driverId = resolveDriverId(authUserId);
        PendingOfferDto offer = readOffer(driverId);
        if (offer == null) {
            return List.of();
        }
        if (isExpired(offer)) {
            cachePort.delete(key(driverId));
            return List.of();
        }
        return List.of(offer);
    }

    /** Driver accepts only after matching confirms this offer won. */
    public void acceptOffer(Long authUserId, Long rideId) {
        PendingOfferDto offer = requireLiveOffer(authUserId, rideId);
        Long vehicleId = vehicleRepository.findAllByDriverId(offer.getDriverId()).stream()
                .filter(VehicleModel::isActive)
                .map(VehicleModel::getId)
                .findFirst()
                .orElse(null);
        String result = matchingClient.accept(rideId, offer.getDriverId(), vehicleId);
        if (!"ACCEPTED".equals(result)) {
            cachePort.delete(key(offer.getDriverId()));
            throw new OfferExpiredException("Offer is no longer available (" + result + ")");
        }
        cachePort.delete(key(offer.getDriverId()));
        cachePort.set(activeRideKey(offer.getDriverId()), rideId);
        log.info("Driver {} accepted offer for ride {}", offer.getDriverId(), rideId);

        try {
            registrationService.changeStatus(authUserId, DriverStatus.BUSY);
        } catch (Exception e) {
            // Matching has already confirmed acceptance; a status hiccup
            // must not report that the driver lost the ride.
            log.warn("Offer accepted but status change failed for driver {}: {}",
                    offer.getDriverId(), e.getMessage());
        }
    }

    /**
     * Drops the staged offer when the matching service withdraws it
     * (another driver accepted first, or the offer window expired).
     * Only removes the entry if it still belongs to this ride.
     */
    public void withdrawOffer(Long driverId, Long rideId) {
        PendingOfferDto offer = readOffer(driverId);
        if (offer != null && rideId.equals(offer.getRideId())) {
            cachePort.delete(key(driverId));
            log.info("Withdrew staged offer: ride={} driver={}", rideId, driverId);
        }
    }

    /** Driver rejects and waits for matching to advance the search. */
    public void rejectOffer(Long authUserId, Long rideId, String reason) {
        PendingOfferDto offer = requireLiveOffer(authUserId, rideId);
        String decisionReason = reason != null && !reason.isBlank() ? reason : "declined";
        String result = matchingClient.reject(rideId, offer.getDriverId(), decisionReason);
        if (!"REJECTED".equals(result) && !"DECLINED".equals(result)) {
            cachePort.delete(key(offer.getDriverId()));
            throw new OfferExpiredException("Offer is no longer available (" + result + ")");
        }
        cachePort.delete(key(offer.getDriverId()));
        log.info("Driver {} rejected offer for ride {} (reason={})",
                offer.getDriverId(), rideId, decisionReason);
    }

    public Long getActiveRideId(Long authUserId) {
        Long driverId = resolveDriverId(authUserId);
        try {
            Long persisted = riderRideLookupClient.activeRideId(driverId);
            if (persisted != null) {
                cachePort.set(activeRideKey(driverId), persisted);
            } else {
                cachePort.delete(activeRideKey(driverId));
            }
            return persisted;
        } catch (RuntimeException ex) {
            log.warn("Rider service unavailable; using cached active ride for driver {}",
                    driverId, ex);
        }
        Object raw = cachePort.get(activeRideKey(driverId));
        if (raw instanceof Number number) return number.longValue();
        if (raw instanceof String value) {
            try { return Long.valueOf(value); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    public void clearActiveRide(Long driverId, Long rideId) {
        Object current = cachePort.get(activeRideKey(driverId));
        if (current != null && rideId.toString().equals(current.toString())) {
            cachePort.delete(activeRideKey(driverId));
            driverRepository.findById(driverId).ifPresent(driver -> {
                if (driver.getDriverStatus() == DriverStatus.BUSY
                        || driver.getDriverStatus() == DriverStatus.ON_TRIP) {
                    try {
                        registrationService.changeStatus(driver.getAuthUserId(), DriverStatus.ONLINE);
                    } catch (Exception ex) {
                        log.warn("Could not restore ONLINE after ride {} for driver {}",
                                rideId, driverId, ex);
                    }
                }
            });
        }
    }

    private PendingOfferDto requireLiveOffer(Long authUserId, Long rideId) {
        Long driverId = resolveDriverId(authUserId);
        PendingOfferDto offer = readOffer(driverId);
        if (offer == null || !rideId.equals(offer.getRideId())) {
            throw new OfferNotFoundException(
                    "No pending offer for ride " + rideId);
        }
        if (isExpired(offer)) {
            cachePort.delete(key(driverId));
            throw new OfferExpiredException(
                    "Offer for ride " + rideId + " already expired");
        }
        return offer;
    }

    private PendingOfferDto readOffer(Long driverId) {
        Object raw = cachePort.get(key(driverId));
        if (raw == null) {
            return null;
        }
        // Redis stores JSON, so this comes back as a Map.
        return objectMapper.convertValue(raw, PendingOfferDto.class);
    }

    private boolean isExpired(PendingOfferDto offer) {
        if (offer.getExpiresAt() == null || offer.getExpiresAt().isBlank()) {
            return false;
        }
        try {
            return Instant.now().isAfter(Instant.parse(offer.getExpiresAt()));
        } catch (Exception e) {
            log.warn("Unparseable offer expiry '{}', treating as live",
                    offer.getExpiresAt());
            return false;
        }
    }

    private Long resolveDriverId(Long authUserId) {
        return driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new OfferNotFoundException(
                        "No driver registered for this user"))
                .getId();
    }

    private String key(Long driverId) {
        return PENDING_OFFER_KEY_PREFIX + driverId;
    }

    private String activeRideKey(Long driverId) {
        return ACTIVE_RIDE_KEY_PREFIX + driverId;
    }
}
