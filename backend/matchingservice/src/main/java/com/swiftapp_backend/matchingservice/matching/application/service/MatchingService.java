package com.swiftapp_backend.matchingservice.matching.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.matchingservice.matching.application.port.MatchingModuleApi;
import com.swiftapp_backend.matchingservice.matching.domain.dto.DriverFindDto;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverLocationEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverOfferAcceptedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverOfferRejectedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideRequestEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideRequestedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverLocation;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverMatchingRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService implements MatchingModuleApi {
    private final DriverMatchingRepository driverMatchingRepository;
    private final MatchingStateRepository matchingStateRepository;
    private final MatchingOrchestrator matchingOrchestrator;
    private final DriverOfferService driverOfferService;
    private final ReassignmentService reassignmentService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveLocation(Object data) {
        DriverLocation location = toModel(toEventFromObject(data));
        driverMatchingRepository.updateDriverLocation(location);
    }

    @Override
    public void updateDriverStatus(Long driverId, DriverStatusEventEnum status) {
        if (driverId != null && status != null) {
            driverMatchingRepository.updateDriverStatus(driverId, status);
        }
    }

    @Override
    public void requestRide(Object data) {
        try {
            RideRequestedEvent event = objectMapper.convertValue(data, RideRequestedEvent.class);
            if (event.getRideId() != null) {
                handleRideRequested(event);
                return;
            }
        } catch (IllegalArgumentException ex) {
            log.debug("Legacy ride request payload without rideId, falling back to lookup-only");
        }
        RideRequestEvent event = toRideRequestEventFromObject(data);
        List<DriverLocation> allDriver = getAllDriver(event.getLatitude(), event.getLongitude(), event.getRadiusInKms(), 10, event.getVehicleTypeEnum());
        log.info("All drivers are {}", allDriver);
    }

    @Override
    public void handleRideRequested(RideRequestedEvent event) {
        matchingOrchestrator.startMatching(event);
    }

    @Override
    public void handleDriverOfferAccepted(DriverOfferAcceptedEvent event) {
        if (event.getRideId() == null || event.getDriverId() == null) {
            log.warn("Ignoring driver-offer-accepted event without rideId/driverId");
            return;
        }
        driverOfferService.acceptOffer(event.getRideId(), event.getDriverId(), event.getVehicleId());
    }

    @Override
    public void handleDriverOfferRejected(DriverOfferRejectedEvent event) {
        if (event.getRideId() == null || event.getDriverId() == null) {
            log.warn("Ignoring driver-offer-rejected event without rideId/driverId");
            return;
        }
        OfferResolution resolution =
                driverOfferService.rejectOffer(event.getRideId(), event.getDriverId(), event.getReason());
        // A rejected sequential offer advances to the next candidate.
        if (resolution == OfferResolution.REJECTED) {
            reassignmentService.reassign(event.getRideId());
        }
    }

    @Override
    public OfferResolution acceptOffer(Long rideId, Long driverId) {
        return driverOfferService.acceptOffer(rideId, driverId);
    }

    @Override
    public OfferResolution acceptOffer(Long rideId, Long driverId, Long vehicleId) {
        return driverOfferService.acceptOffer(rideId, driverId, vehicleId);
    }

    @Override
    public OfferResolution rejectOffer(Long rideId, Long driverId, String reason) {
        OfferResolution resolution = driverOfferService.rejectOffer(rideId, driverId, reason);
        // A rejected sequential offer advances to the next candidate.
        if (resolution == OfferResolution.REJECTED) {
            reassignmentService.reassign(rideId);
        }
        return resolution;
    }

    @Override
    public DriverOfferRepository.CancelOutcome cancelPendingRide(Long rideId) {
        return driverOfferService.cancelPendingRide(rideId);
    }

    @Override
    public void recordRideCancelled(Long rideId) {
        driverOfferService.recordRideCancelled(rideId);
    }

    @Override
    public Optional<MatchingState> matchingState(Long rideId) {
        return matchingStateRepository.findByRideId(rideId);
    }

    @Transactional(readOnly = true)
    public List<DriverLocation> getAllDriver(double latitude, double longitude, double kms, int limit, VehicleTypeEnum vehicleType) {
        DriverFindDto dto = DriverFindDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radiusInKilometers(kms)
                .limit(limit)
                .vehicleTypeEnum(vehicleType)
                .build();
        return driverMatchingRepository.findAllNearbyDriver(dto);
    }

    private DriverLocation toModel(DriverLocationEvent event) {
        return DriverLocation
                .builder()
                .driverId(event.getDriverId())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .statusEvent(event.getDriverStatus())
                .vehicleType(event.getVehicleTypeEnum())
                .build();
    }

    private RideRequestEvent toRideRequestEventFromObject(Object data) {
        return objectMapper.convertValue(data, RideRequestEvent.class);
    }

    private DriverLocationEvent toEventFromObject(Object data) {
        return objectMapper.convertValue(data, DriverLocationEvent.class);
    }
}
