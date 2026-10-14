package com.swiftapp_backend.matchingservice.matching;

import com.swiftapp_backend.matchingservice.matching.application.service.DriverOfferService;
import com.swiftapp_backend.matchingservice.matching.application.service.MatchingService;
import com.swiftapp_backend.matchingservice.matching.application.service.OfferResolution;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStatus;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideRequestedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideMatchFailedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingStateRepository;
import com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.entity.DriverLocationEntity;
import com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.repository.DriverMatchingRedisRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Live test against the real Redis on localhost:6379.
 *
 * <p>Kafka is intentionally absent here: the publisher now degrades
 * gracefully (logs and continues), so the whole matching flow can be
 * verified end to end - GEO search, Lua lock, offer hash, timeout ZSET,
 * accept and lock release - with only Redis running.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "matching.outbox-retry-interval-ms=600000"
})
class MatchingLiveRedisIT {

    private static final Long DRIVER_ID = 90001L;
    private static final Long RIDE_ID = 91001L;
    private static final Long FAILED_RIDE_ID = 91002L;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private DriverOfferService driverOfferService;

    @Autowired
    private DriverMatchingRedisRepository driverMatchingRedisRepository;

    @Autowired
    private MatchingStateRepository matchingStateRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        redisTemplate.opsForZSet().remove("drivers:geo", DRIVER_ID.toString());
        redisTemplate.delete("driver-matching" + DRIVER_ID);
        redisTemplate.opsForHash().delete("drivers:last-seen", DRIVER_ID.toString());
        redisTemplate.delete("ride:offer:" + RIDE_ID);
        redisTemplate.delete("ride:matching:" + RIDE_ID);
        redisTemplate.delete("ride:attempted-drivers:" + RIDE_ID);
        redisTemplate.opsForZSet().remove("matching:timeouts", RIDE_ID.toString());
        redisTemplate.delete("driver:matching-lock:" + DRIVER_ID);
        redisTemplate.delete("matching:timeout-claim:" + RIDE_ID);
        redisTemplate.opsForHash().delete("matching:outbox:driver-matched:payloads",
                RIDE_ID.toString());
        redisTemplate.opsForSet().remove("matching:outbox:driver-matched:pending",
                RIDE_ID.toString());
        redisTemplate.delete("ride:matching:" + FAILED_RIDE_ID);
        redisTemplate.delete("ride:attempted-drivers:" + FAILED_RIDE_ID);
        redisTemplate.opsForHash().delete("matching:outbox:match-failed:payloads",
                FAILED_RIDE_ID.toString());
        redisTemplate.opsForSet().remove("matching:outbox:match-failed:pending",
                FAILED_RIDE_ID.toString());
    }

    @Test
    void fullOfferFlowAgainstRealRedis() throws Exception {
        // 1. Seed an ACTIVE driver right next to the pickup point.
        driverMatchingRedisRepository.updateDriverLocation(
                DriverLocationEntity.builder()
                        .driverId(DRIVER_ID)
                        .latitude(12.9716)
                        .longitude(77.5946)
                        .driverStatus(DriverStatusEventEnum.ACTIVE)
                        .vehicleType(VehicleTypeEnum.SEDAN)
                        .build());

        // 2. riderservice-shaped payload, including fields matchingservice
        //    does not know - they must be ignored, not blow up.
        String payload = """
                {"rideId":%d,"riderId":77,"pickupLatitude":12.9716,"pickupLongitude":77.5946,
                 "fare":250.5,"otp":"1234","extra":{"nested":true}}""".formatted(RIDE_ID);
        RideRequestedEvent event =
                objectMapper.readValue(payload, RideRequestedEvent.class);
        assertEquals(RIDE_ID, event.getRideId());

        // 3. Run the real flow.
        matchingService.handleRideRequested(event);

        // 4. Offer exists in Redis, driver lock is held by this ride,
        //    and the timeout is scheduled in the sorted set.
        MatchingState state = matchingStateRepository.findByRideId(RIDE_ID)
                .orElseThrow();
        assertEquals(MatchingStatus.OFFER_SENT, state.getStatus());
        assertNull(state.getDriverId());

        assertTrue(Boolean.TRUE.equals(
                redisTemplate.hasKey("ride:offer:" + RIDE_ID)));
        assertEquals(RIDE_ID.toString(),
                redisTemplate.opsForValue().get("driver:matching-lock:" + DRIVER_ID));
        assertNotNull(redisTemplate.opsForZSet()
                .score("matching:timeouts", RIDE_ID.toString()));

        // 5. Driver accepts through the real service.
        assertEquals(OfferResolution.ACCEPTED,
                driverOfferService.acceptOffer(RIDE_ID, DRIVER_ID));

        // 6. Ride is matched, lock released, timeout unscheduled.
        assertEquals(MatchingStatus.MATCHED, matchingStateRepository
                .findByRideId(RIDE_ID).orElseThrow().getStatus());
        assertNull(redisTemplate.opsForValue()
                .get("driver:matching-lock:" + DRIVER_ID));
        assertNull(redisTemplate.opsForZSet()
                .score("matching:timeouts", RIDE_ID.toString()));
    }

    @Test
    void noAvailableDriverStoresFailureWithRetryableEventInRedis() {
        matchingStateRepository.saveFailed(MatchingState.builder()
                        .rideId(FAILED_RIDE_ID).riderId(77L)
                        .status(MatchingStatus.FAILED).build(),
                new RideMatchFailedEvent(FAILED_RIDE_ID, 77L, "NO_AVAILABLE_DRIVER"));

        assertEquals(MatchingStatus.FAILED, matchingStateRepository
                .findByRideId(FAILED_RIDE_ID).orElseThrow().getStatus());
        assertTrue(matchingStateRepository.pendingFailedEvents(100).stream()
                .anyMatch(failed -> FAILED_RIDE_ID.equals(failed.rideId())
                        && Long.valueOf(77L).equals(failed.riderId())
                        && "NO_AVAILABLE_DRIVER".equals(failed.reason())));
        matchingStateRepository.acknowledgeFailedEvent(FAILED_RIDE_ID);
        assertFalse(matchingStateRepository.pendingFailedEvents(100).stream()
                .anyMatch(failed -> FAILED_RIDE_ID.equals(failed.rideId())));
    }
}
