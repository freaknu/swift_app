package com.swiftapp_backend.matchingservice.matching.support;

import com.swiftapp_backend.matchingservice.matching.application.config.MatchingProperties;
import com.swiftapp_backend.matchingservice.matching.application.service.DriverLockService;
import com.swiftapp_backend.matchingservice.matching.application.service.DriverOfferService;
import com.swiftapp_backend.matchingservice.matching.application.service.DriverSelectionService;
import com.swiftapp_backend.matchingservice.matching.application.service.MatchingOrchestrator;
import com.swiftapp_backend.matchingservice.matching.application.service.ReassignmentService;
import com.swiftapp_backend.matchingservice.matching.application.strategy.NearestDriverStrategy;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStatus;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStrategyType;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;

import java.time.Instant;
import java.util.List;

/** Wires the real services with in-memory fakes. */
public class MatchingTestHarness {

    public final MatchingProperties properties = new MatchingProperties();

    public final InMemoryDriverOfferRepository offerRepo = new InMemoryDriverOfferRepository();

    public final InMemoryMatchingStateRepository stateRepo = new InMemoryMatchingStateRepository();

    public final InMemoryDriverLockRepository lockRepo = new InMemoryDriverLockRepository();

    public final InMemoryMatchingTimeoutRepository timeoutRepo = new InMemoryMatchingTimeoutRepository();

    public final StubDriverMatchingRepository driverRepo = new StubDriverMatchingRepository();

    public final StubDriverCandidateRepository candidateRepo = new StubDriverCandidateRepository();

    public final RecordingMatchingEventPublisher publisher = new RecordingMatchingEventPublisher();

    public final DriverSelectionService selectionService;

    public final DriverLockService lockService;

    public final DriverOfferService offerService;

    public final ReassignmentService reassignmentService;

    public final MatchingOrchestrator orchestrator;

    public MatchingTestHarness() {
        properties.setOfferTtlSeconds(15);
        properties.setLockTtlSeconds(20);
        properties.setMaxAttempts(4);
        properties.setRadiiKm(List.of(5.0, 7.0, 10.0, 15.0));
        properties.setStrategy(MatchingStrategyType.NEAREST);

        selectionService =
                new DriverSelectionService(List.of(new NearestDriverStrategy()), properties);
        lockService = new DriverLockService(lockRepo, properties);
        offerService = new DriverOfferService(offerRepo, stateRepo, lockRepo,
                timeoutRepo, driverRepo, publisher, properties);
        reassignmentService = new ReassignmentService(stateRepo, candidateRepo,
                selectionService, lockService, offerService, publisher, properties);
        orchestrator = new MatchingOrchestrator(stateRepo, offerRepo, reassignmentService);
    }

    public MatchingState newState(Long rideId) {
        Instant now = Instant.now();
        MatchingState state = MatchingState.builder()
                .rideId(rideId)
                .riderId(7L)
                .pickupLatitude(12.9716)
                .pickupLongitude(77.5946)
                .vehicleType(VehicleTypeEnum.SEDAN)
                .status(MatchingStatus.SEARCHING)
                .attempt(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        stateRepo.save(state);
        return state;
    }

    public static DriverCandidate candidate(Long driverId, double distanceKm) {
        return DriverCandidate.builder()
                .driverId(driverId)
                .latitude(12.9716)
                .longitude(77.5946)
                .distanceKm(distanceKm)
                .status(DriverStatusEventEnum.ACTIVE)
                .vehicleType(VehicleTypeEnum.SEDAN)
                .lastSeenEpochMilli(System.currentTimeMillis())
                .build();
    }
}
