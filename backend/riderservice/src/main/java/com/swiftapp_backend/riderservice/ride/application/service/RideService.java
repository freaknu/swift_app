package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateDto;
import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateRequest;
import com.swiftapp_backend.riderservice.ride.domain.event.*;
import com.swiftapp_backend.riderservice.ride.api.dto.location.RecentLocationDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.AcceptRideRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.CancelRideRequest;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideRequestDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideStatusHistoryDto;
import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.enums.PaymentStatus;
import com.swiftapp_backend.riderservice.ride.domain.enums.VehicleTypeCode;
import com.swiftapp_backend.riderservice.ride.application.mapper.FareMapper;
import com.swiftapp_backend.riderservice.ride.application.mapper.RecentLocationMapper;
import com.swiftapp_backend.riderservice.ride.application.mapper.RideHistoryMapper;
import com.swiftapp_backend.riderservice.ride.application.mapper.RideMapper;
import com.swiftapp_backend.riderservice.ride.domain.model.FareDetailModel;
import com.swiftapp_backend.riderservice.ride.domain.model.PageResult;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;
import com.swiftapp_backend.riderservice.ride.domain.model.RideStatusHistoryModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.FareDetailRepository;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideRepository;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideStatusHistoryRepository;
import com.swiftapp_backend.riderservice.ride.application.port.MatchingCancellationPort;
import com.swiftapp_backend.riderservice.shared.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideService {
    private static final Set<RideStatus> ACTIVE_STATUSES = EnumSet.of(
            RideStatus.REQUESTED, RideStatus.MATCHING, RideStatus.DRIVER_ASSIGNED,
            RideStatus.DRIVER_ARRIVED, RideStatus.STARTED);

    private final RideRepository rideRepository;
    private final RideStatusHistoryRepository rideStatusHistoryRepository;
    private final FareDetailRepository fareDetailRepository;
    private final FareService fareService;
    private final FareMapper fareMapper;
    private final RideMapper rideMapper;
    private final RideHistoryMapper rideHistoryMapper;
    private final RecentLocationMapper recentLocationMapper;
    private final KafkaRideService kafkaRideService;
    private final MatchingCancellationPort matchingCancellationPort;

    @Transactional
    public RideDto requestRide(RideRequestDto request) {
        VehicleTypeCode vehicleType;
        try {
            vehicleType = VehicleTypeCode.fromRequest(request.rideType());
            if (request.route() != null && request.route().rideType() != null
                    && VehicleTypeCode.fromRequest(request.route().rideType()) != vehicleType) {
                throw ApiException.badRequest("Route rideType must match selected vehicle type");
            }
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unsupported vehicle type: " + request.rideType());
        }
        if (request.route() == null) {
            throw ApiException.badRequest("Pickup and destination coordinates are required");
        }
        if (rideRepository.existsByRiderIdAndCurrentStatusIn(request.riderId(), ACTIVE_STATUSES)) {
            throw ApiException.conflict("Rider already has an active ride");
        }

        FareEstimateRequest route = request.route();
        FareEstimateDto estimate = fareService.estimate(new FareEstimateRequest(
                route.offerId(), route.pickupLatitude(), route.pickupLongitude(),
                route.destinationLatitude(), route.destinationLongitude(), vehicleType.name()));

        LocalDateTime now = LocalDateTime.now();
        RideModel ride = RideModel.builder()
                .riderId(request.riderId())
                .activeRiderId(request.riderId())
                .pickupLatitude(request.route().pickupLatitude())
                .pickupLongitude(request.route().pickupLongitude())
                .destinationLatitude(request.route().destinationLatitude())
                .destinationLongitude(request.route().destinationLongitude())
                .estimatedDistance(estimate.estimatedDistanceKm())
                .estimatedDuration(estimate.estimatedDurationSeconds())
                .rideType(vehicleType.storedRideType())
                .rideTypeCode(vehicleType.name())
                .requestedAt(now)
                .currentStatus(RideStatus.REQUESTED)
                .build();
        ride = rideRepository.save(ride);

        FareDetailModel fare = fareMapper.toModel(estimate.fare());
        fare.setRide(ride);
        fare = fareDetailRepository.save(fare);
        ride.setFareDetail(fare);
        recordStatus(ride, RideStatus.REQUESTED, request.riderId(), ChangedByType.RIDER,
                "Ride requested by rider");
        recordStatus(ride, RideStatus.MATCHING, null, ChangedByType.SYSTEM,
                "Searching for an available driver");
        ride = rideRepository.save(ride);

        // Start matching with the exact vehicle category selected by the rider.
        publishRideRequested(ride);
        publishApplyOffer(ride,request.authUserId(),request.offerId());

        return rideMapper.toDto(ride);
    }
    @Transactional
    public RideDto acceptRide(Long rideId, AcceptRideRequest request) {
        RideModel ride = lockedRide(rideId);
        requireStatus(ride, RideStatus.REQUESTED, RideStatus.MATCHING);
        if (rideRepository.existsByDriverIdAndCurrentStatusIn(request.driverId(), ACTIVE_STATUSES)) {
            throw ApiException.conflict("Driver already has an active ride");
        }
        ride.setDriverId(request.driverId());
        ride.setActiveDriverId(request.driverId());
        ride.setVehicleId(request.vehicleId());
        ride.setAcceptedAt(LocalDateTime.now());
        recordStatus(ride, RideStatus.DRIVER_ASSIGNED, request.driverId(), ChangedByType.DRIVER,
                "Ride accepted by driver");
        ride = rideRepository.save(ride);

        // broadcast the driver assignment ->
        publishRideAccepted(ride);
        return rideMapper.toDto(ride);
    }

    /** Applies the matching service's winning decision to the persisted ride.
     * Kafka may redeliver this event after the ride has advanced or ended. */
    @Transactional
    public RideDto assignMatchedDriver(Long rideId, Long driverId, Long vehicleId) {
        RideModel ride = lockedRide(rideId);
        if (driverId.equals(ride.getDriverId())
                && Objects.equals(vehicleId, ride.getVehicleId())
                && EnumSet.of(RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVED,
                        RideStatus.STARTED, RideStatus.COMPLETED, RideStatus.CANCELLED)
                        .contains(ride.getCurrentStatus())) {
            return rideMapper.toDto(ride);
        }
        requireStatus(ride, RideStatus.REQUESTED, RideStatus.MATCHING);
        if (rideRepository.existsByDriverIdAndCurrentStatusIn(driverId, ACTIVE_STATUSES)) {
            throw ApiException.conflict("Driver already has an active ride");
        }
        ride.setDriverId(driverId);
        ride.setActiveDriverId(driverId);
        ride.setVehicleId(vehicleId);
        ride.setAcceptedAt(LocalDateTime.now());
        recordStatus(ride, RideStatus.DRIVER_ASSIGNED, driverId, ChangedByType.DRIVER,
                "Ride accepted by driver through matching");
        ride = rideRepository.save(ride);
        publishRideAccepted(ride);
        return rideMapper.toDto(ride);
    }

    /** Ends an unsuccessful search and frees the rider to request another ride.
     * Duplicate or late matching failure messages cannot undo an assignment. */
    @Transactional
    public RideDto failMatching(Long rideId, Long riderId, String reason) {
        RideModel ride = lockedRide(rideId);
        if (riderId == null || !riderId.equals(ride.getRiderId())) {
            // Kafka may retain an older matching event after the database was
            // replaced or restored. A colliding ride ID must not affect the
            // current rider, and retrying the stale event cannot fix it.
            log.warn("Ignoring matching failure for ride {}: event rider {} differs from ride rider {}",
                    rideId, riderId, ride.getRiderId());
            return rideMapper.toDto(ride);
        }
        if (ride.getCurrentStatus() == RideStatus.MATCH_FAILED) {
            return rideMapper.toDto(ride);
        }
        if (ride.getCurrentStatus() != RideStatus.MATCHING
                && ride.getCurrentStatus() != RideStatus.REQUESTED) {
            log.info("Ignoring late matching failure for ride {} in status {}",
                    rideId, ride.getCurrentStatus());
            return rideMapper.toDto(ride);
        }
        ride.setActiveRiderId(null);
        recordStatus(ride, RideStatus.MATCH_FAILED, null, ChangedByType.SYSTEM,
                "NO_AVAILABLE_DRIVER".equals(reason)
                        ? "No drivers are available right now. Please try again."
                        : "Driver search ended. Please try again.");
        return rideMapper.toDto(rideRepository.save(ride));
    }

    @Transactional
    public RideDto driverArrived(Long rideId, Long driverId) {
        RideModel ride = lockedRide(rideId);
        requireAssignedDriver(ride, driverId);
        requireStatus(ride, RideStatus.DRIVER_ASSIGNED);
        ride.setDriverArrivedAt(LocalDateTime.now());
        recordStatus(ride, RideStatus.DRIVER_ARRIVED, driverId, ChangedByType.DRIVER,
                "Driver arrived at pickup");
        ride = rideRepository.save(ride);

        // broadcast driver arrival ->
        publishDriverArrived(ride);
        return rideMapper.toDto(ride);
    }

    @Transactional
    public RideDto startRide(Long rideId, Long driverId) {
        RideModel ride = lockedRide(rideId);
        requireAssignedDriver(ride, driverId);
        requireStatus(ride, RideStatus.DRIVER_ARRIVED);
        ride.setStartedAt(LocalDateTime.now());
        recordStatus(ride, RideStatus.STARTED, driverId, ChangedByType.DRIVER, "Ride started");
        ride = rideRepository.save(ride);

        // broadcast ride start ->
        publishRideStarted(ride);
        return rideMapper.toDto(ride);
    }

    @Transactional
    public RideDto completeRide(Long rideId, Long driverId) {
        RideModel ride = lockedRide(rideId);
        requireAssignedDriver(ride, driverId);
        requireStatus(ride, RideStatus.STARTED);
        LocalDateTime completedAt = LocalDateTime.now();
        ride.setCompletedAt(completedAt);
        ride.setIsCompleted(true);
        ride.setActualDistance(ride.getEstimatedDistance());
        ride.setActualDuration(Math.max(0, Duration.between(ride.getStartedAt(), completedAt).toSeconds()));
        ride.setActiveRiderId(null);
        ride.setActiveDriverId(null);
        recordStatus(ride, RideStatus.COMPLETED, driverId, ChangedByType.DRIVER, "Ride completed");
        ride = rideRepository.save(ride);

        handleRideCompleted(ride);
        // broadcast ride completion ->
        publishRideCompleted(ride);
        return rideMapper.toDto(ride);
    }

    @Transactional
    public RideDto cancelRide(Long rideId, CancelRideRequest request) {
        RideModel ride = lockedRide(rideId);
        if (!ACTIVE_STATUSES.contains(ride.getCurrentStatus())) {
            throw ApiException.conflict("A " + ride.getCurrentStatus() + " ride cannot be cancelled");
        }
        if (ride.getCurrentStatus() == RideStatus.STARTED) {
            throw ApiException.conflict("A started ride must be completed or handled by support");
        }

        validateCancellationActor(ride, request);
        if (ride.getCurrentStatus() == RideStatus.REQUESTED
                || ride.getCurrentStatus() == RideStatus.MATCHING) {
            MatchingCancellationPort.Result outcome;
            try {
                outcome = matchingCancellationPort.cancelPendingRide(rideId);
            } catch (RuntimeException ex) {
                log.warn("Matching cancellation unavailable for ride {}", rideId, ex);
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Could not confirm ride cancellation. Please try again.");
            }
            if (outcome == MatchingCancellationPort.Result.ALREADY_ACCEPTED) {
                throw ApiException.conflict("A driver accepted this ride. Please retry after assignment completes.");
            }
        }
        ride.setCancelledAt(LocalDateTime.now());
        ride.setCancelledBy(request.actorId());
        ride.setCancellationReason(request.reason().trim());
        ride.setIsRideCancelled(true);
        ride.setActiveRiderId(null);
        ride.setActiveDriverId(null);
        recordStatus(ride, RideStatus.CANCELLED, request.actorId(), request.actorType(),
                request.reason().trim());
        ride = rideRepository.save(ride);

        // broadcast cancellation ->
        publishRideCancelled(ride, request.actorType());
        return rideMapper.toDto(ride);
    }

    @Transactional(readOnly = true)
    public RideDto getRide(Long rideId) {
        return rideMapper.toDto(findRide(rideId));
    }

    @Transactional
    public void recordPaymentCompleted(Long rideId, Long riderId, Long driverId,
                                       Long amount, String currency, String paymentId) {
        RideModel ride = lockedRide(rideId);
        FareDetailModel fare = ride.getFareDetail();
        if (ride.getCurrentStatus() != RideStatus.COMPLETED || fare == null
                || !Objects.equals(ride.getRiderId(), riderId)
                || !Objects.equals(ride.getDriverId(), driverId)
                || amount == null || amount <= 0 || fare.getTotalFare() == null
                || fare.getCurrency() == null || !fare.getCurrency().name().equals(currency)
                || fare.getTotalFare().movePointRight(2).compareTo(java.math.BigDecimal.valueOf(amount)) != 0
                || paymentId == null || paymentId.isBlank()) {
            throw ApiException.conflict("Payment event does not match the completed ride fare");
        }
        if (fare.getPaymentStatus() != PaymentStatus.SUCCESS) {
            fare.setPaymentStatus(PaymentStatus.SUCCESS);
            fareDetailRepository.save(fare);
        }
    }

    @Transactional(readOnly = true)
    public RideDto getActiveRide(Long riderId) {
        RideModel ride = rideRepository.findFirstByRiderIdAndCurrentStatusInOrderByRequestedAtDesc(
                        riderId, ACTIVE_STATUSES)
                .orElseThrow(() -> ApiException.notFound("No active ride found for rider " + riderId));
        return rideMapper.toDto(ride);
    }

    @Transactional(readOnly = true)
    public Long getActiveDriverRideId(Long driverId) {
        return rideRepository.findFirstByDriverIdAndCurrentStatusInOrderByRequestedAtDesc(
                driverId, ACTIVE_STATUSES).map(RideModel::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResult<RideDto> getRiderRides(Long riderId, int page, int size) {
        return rideRepository.findAllByRiderIdOrderByRequestedAtDesc(riderId, page, size)
                .map(rideMapper::toDto);
    }

    @Transactional(readOnly = true)
    public PageResult<RideDto> getDriverRides(Long driverId, int page, int size) {
        return rideRepository.findAllByDriverIdOrderByRequestedAtDescIdDesc(driverId, page, size)
                .map(rideMapper::toDto);
    }

    @Transactional(readOnly = true)
    public PageResult<RideDto> getDriverRides(Long driverId, int page, int size, RideStatus status) {
        return status == null ? getDriverRides(driverId, page, size)
                : rideRepository.findAllByDriverIdAndCurrentStatusOrderByRequestedAtDescIdDesc(
                        driverId, status, page, size).map(rideMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<RideStatusHistoryDto> getRideHistory(Long rideId) {
        if (!rideRepository.existsById(rideId)) {
            throw ApiException.notFound("Ride not found: " + rideId);
        }
        return rideStatusHistoryRepository.findAllByRideIdOrderByCreatedAtAsc(rideId)
                .stream().map(rideHistoryMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RecentLocationDto> getRideLocationSuggestions(Long userId,double latitude,double longitude) {
        return rideRepository.findPreviousDestinations(userId, latitude, longitude, 100)
                .stream()
                .map(recentLocationMapper::toDto)
                .toList();
    }

    @Async
    private void handleRideCompleted(RideModel ride) {
        RideHistoryCreateEvent data = RideHistoryCreateEvent.builder()
                .riderId(ride.getRiderId())
                .rideId(ride.getId())
                .pickUpLatitude(ride.getPickupLatitude())
                .pickUpLongitude(ride.getPickupLongitude())
                .destinationLatitude(ride.getDestinationLatitude())
                .destinationLongitude(ride.getDestinationLongitude())
                .build();
        kafkaRideService.publishRideHistoryCreate(data);
    }
    private RideModel findRide(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> ApiException.notFound("Ride not found: " + rideId));
    }

    private RideModel lockedRide(Long rideId) {
        return rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> ApiException.notFound("Ride not found: " + rideId));
    }

    private void recordStatus(RideModel ride, RideStatus status, Long actorId,
                              ChangedByType actorType, String remarks) {
        RideStatusHistoryModel history = RideStatusHistoryModel.builder()
                .ride(ride)
                .rideStatus(status)
                .changedById(actorId)
                .changedByType(actorType)
                .remarks(remarks)
                .build();
        RideStatusHistoryModel savedHistory = rideStatusHistoryRepository.save(history);
        ride.getRideHistory().add(savedHistory);
        ride.setCurrentStatus(status);
    }

    private void requireStatus(RideModel ride, RideStatus... allowed) {
        for (RideStatus status : allowed) {
            if (ride.getCurrentStatus() == status) return;
        }
        throw ApiException.conflict("Ride action is not allowed while status is " + ride.getCurrentStatus());
    }

    private void requireAssignedDriver(RideModel ride, Long driverId) {
        if (ride.getDriverId() == null || !ride.getDriverId().equals(driverId)) {
            throw ApiException.forbidden("Driver is not assigned to this ride");
        }
    }

    private void validateCancellationActor(RideModel ride, CancelRideRequest request) {
        if (request.actorType() == ChangedByType.RIDER) {
            if (!ride.getRiderId().equals(request.actorId())) {
                throw ApiException.forbidden("Rider does not own this ride");
            }
            return;
        }
        if (request.actorType() == ChangedByType.DRIVER) {
            requireAssignedDriver(ride, request.actorId());
            return;
        }
        throw ApiException.badRequest("Only the assigned rider or driver can cancel through this endpoint");
    }


    private void publishRideRequested(RideModel ride) {
        FareDetailModel fare = ride.getFareDetail();
        RideRequestedEventDto event = RideRequestedEventDto.builder()
                .rideId(ride.getId())
                .riderId(ride.getRiderId())
                .driverId(ride.getDriverId())
                .vehicleId(ride.getVehicleId())
                .rideStatus(ride.getCurrentStatus())
                .eventTime(LocalDateTime.now())
                .pickupLatitude(ride.getPickupLatitude())
                .pickupLongitude(ride.getPickupLongitude())
                .rideType(ride.getRideType())
                .vehicleType(ride.getRideTypeCode())
                .destinationLatitude(ride.getDestinationLatitude())
                .destinationLongitude(ride.getDestinationLongitude())
                .estimatedDistance(ride.getEstimatedDistance())
                .estimatedDuration(ride.getEstimatedDuration())
                .baseFare(fare != null ? fare.getBaseFare() : null)
                .distanceFare(fare != null ? fare.getDistanceFare() : null)
                .timeFare(fare != null ? fare.getTimeFare() : null)
                .bookingFee(fare != null ? fare.getBookingFee() : null)
                .tax(fare != null ? fare.getTax() : null)
                .surgeMultiplier(fare != null ? fare.getSurgeMultiplier() : null)
                .discount(fare != null ? fare.getDiscount() : null)
                .totalFare(fare != null ? fare.getTotalFare() : null)
                .currency(fare != null ? fare.getCurrency() : null)
                .build();
        kafkaRideService.publishRideRequested(event);
    }

    private void publishRideAccepted(RideModel ride) {
        RideAcceptedEventDto event = RideAcceptedEventDto.builder()
                .rideId(ride.getId())
                .riderId(ride.getRiderId())
                .driverId(ride.getDriverId())
                .vehicleId(ride.getVehicleId())
                .rideStatus(ride.getCurrentStatus())
                .eventTime(LocalDateTime.now())
                .acceptedAt(ride.getAcceptedAt())
                .pickupLatitude(ride.getPickupLatitude())
                .pickupLongitude(ride.getPickupLongitude())
                .destinationLatitude(ride.getDestinationLatitude())
                .destinationLongitude(ride.getDestinationLongitude())
                .rideType(ride.getRideTypeCode())
                .build();
        kafkaRideService.publishRideAssigned(event);
    }

    private void publishDriverArrived(RideModel ride) {
        DriverArrivedEventDto event = DriverArrivedEventDto.builder()
                .rideId(ride.getId())
                .riderId(ride.getRiderId())
                .driverId(ride.getDriverId())
                .vehicleId(ride.getVehicleId())
                .rideStatus(ride.getCurrentStatus())
                .eventTime(LocalDateTime.now())
                .arrivedAt(ride.getDriverArrivedAt())
                .build();
        kafkaRideService.publishDriverArrived(event);
    }

    private void publishRideStarted(RideModel ride) {
        RideStartedEventDto event = RideStartedEventDto.builder()
                .rideId(ride.getId())
                .riderId(ride.getRiderId())
                .driverId(ride.getDriverId())
                .vehicleId(ride.getVehicleId())
                .rideStatus(ride.getCurrentStatus())
                .eventTime(LocalDateTime.now())
                .startedAt(ride.getStartedAt())
                .build();
        kafkaRideService.publishRideStarted(event);
    }

    private void publishRideCompleted(RideModel ride) {
        FareDetailModel fare = ride.getFareDetail();
        RideCompletedEventDto event = RideCompletedEventDto.builder()
                .rideId(ride.getId())
                .riderId(ride.getRiderId())
                .driverId(ride.getDriverId())
                .vehicleId(ride.getVehicleId())
                .rideStatus(ride.getCurrentStatus())
                .eventTime(LocalDateTime.now())
                .actualDistance(ride.getActualDistance())
                .actualDuration(ride.getActualDuration())
                .totalFare(fare != null ? fare.getTotalFare() : null)
                .currency(fare != null ? fare.getCurrency() : null)
                .completedAt(ride.getCompletedAt())
                .build();
        kafkaRideService.publishRideCompleted(event);
    }

    private void publishRideCancelled(RideModel ride, ChangedByType cancelledByType) {
        RideCancelledEventDto event = RideCancelledEventDto.builder()
                .rideId(ride.getId())
                .riderId(ride.getRiderId())
                .driverId(ride.getDriverId())
                .vehicleId(ride.getVehicleId())
                .rideStatus(ride.getCurrentStatus())
                .eventTime(LocalDateTime.now())
                .cancelledBy(ride.getCancelledBy())
                .cancelledByType(cancelledByType)
                .reason(ride.getCancellationReason())
                .cancelledAt(ride.getCancelledAt())
                .build();
        kafkaRideService.publishRideCancelled(event);
    }

    private void publishApplyOffer(RideModel ride,Long authUserId,Long offerId) {
        if (offerId == null) {
            return;
        }
        ApplyOfferRequestEvent data = ApplyOfferRequestEvent
                .builder()
                .authUserId(authUserId)
                .offerId(offerId)
                .riderId(ride.getRiderId())
                .build();
        kafkaRideService.publishApplyOfferCreate(data);
    }
}
