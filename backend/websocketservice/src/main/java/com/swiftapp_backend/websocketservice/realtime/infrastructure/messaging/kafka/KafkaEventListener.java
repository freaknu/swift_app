package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.websocketservice.realtime.application.service.RealtimeEventService;
import com.swiftapp_backend.websocketservice.realtime.application.service.DriverRideLightweightService;
import com.swiftapp_backend.websocketservice.realtime.domain.event.DriverArrivedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.LocationUpdateEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideAcceptedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideCancelledEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideCompletedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideRequestedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideOfferEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideOfferWithdrawnEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideSearchProgressEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideMatchFailedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideStartedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.enums.RideStatus;
import com.swiftapp_backend.websocketservice.realtime.domain.model.NearbyDriver;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.DRIVER_ARRIVED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.DRIVER_BROADCAST;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_OFFER_BROADCAST;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_OFFER_WITHDRAWN;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_MATCH_FAILED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.LOCATION_UPDATED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.LOCATION_UPDATE;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_ASSIGNED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_CANCELLED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_COMPLETED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_REQUESTED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_STARTED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_REQUESTED_DOT;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_ASSIGNED_DOT;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.DRIVER_ASSIGNED_DOT;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.DRIVER_ARRIVED_DOT;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_STARTED_DOT;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_COMPLETED_DOT;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.RIDE_CANCELLED_DOT;

@Service
@Slf4j
public class KafkaEventListener {

    private final RealtimeEventService realtimeEventService;
    private final ObjectMapper objectMapper;
    private final DriverRideLightweightService driverRideLightweightService;
    private final ConcurrentMap<String, Long> relayedLocationEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> relayedRideEvents = new ConcurrentHashMap<>();

    @Autowired
    public KafkaEventListener(RealtimeEventService realtimeEventService,
                              ObjectMapper objectMapper,
                              DriverRideLightweightService driverRideLightweightService) {
        this.realtimeEventService = realtimeEventService;
        this.objectMapper = objectMapper;
        this.driverRideLightweightService = driverRideLightweightService;
    }

    /** Backwards-compatible constructor for listener unit tests that only
     * exercise the Kafka-to-WebSocket relay. */
    public KafkaEventListener(RealtimeEventService realtimeEventService,
                              ObjectMapper objectMapper) {
        this(realtimeEventService, objectMapper, null);
    }

    @KafkaListener(
            topics = {
                    RIDE_REQUESTED,
                    RIDE_ASSIGNED,
                    DRIVER_ARRIVED,
                    RIDE_STARTED,
                    RIDE_COMPLETED,
                    RIDE_CANCELLED,
                    RIDE_REQUESTED_DOT,
                    RIDE_ASSIGNED_DOT,
                    DRIVER_ASSIGNED_DOT,
                    DRIVER_ARRIVED_DOT,
                    RIDE_STARTED_DOT,
                    RIDE_COMPLETED_DOT,
                     RIDE_CANCELLED_DOT,
                     LOCATION_UPDATE,
                     LOCATION_UPDATED,
                    DRIVER_BROADCAST,
                    RIDE_OFFER_BROADCAST,
                    RIDE_OFFER_WITHDRAWN,
                    RIDE_MATCH_FAILED
            },
            groupId = "websocket-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {

        try {

            Object dto = record.value();
            String topic = record.topic();
            log.debug("Relaying Kafka topic {}", topic);


            switch (topic) {
                case RIDE_REQUESTED, RIDE_REQUESTED_DOT ->
                        relayRideOnce("REQUESTED", dto, () -> rideRequest(dto));
                case RIDE_ASSIGNED, RIDE_ASSIGNED_DOT, DRIVER_ASSIGNED_DOT ->
                        relayRideOnce("DRIVER_ASSIGNED", dto, () -> rideAssigned(dto));
                case DRIVER_ARRIVED, DRIVER_ARRIVED_DOT ->
                        relayRideOnce("DRIVER_ARRIVED", dto, () -> rideArrived(dto));
                case RIDE_STARTED, RIDE_STARTED_DOT ->
                        relayRideOnce("STARTED", dto, () -> rideStarted(dto));
                case RIDE_COMPLETED, RIDE_COMPLETED_DOT ->
                        relayRideOnce("COMPLETED", dto, () -> rideCompleted(dto));
                case RIDE_CANCELLED, RIDE_CANCELLED_DOT ->
                        relayRideOnce("CANCELLED", dto, () -> rideCancelled(dto));
                case LOCATION_UPDATE, LOCATION_UPDATED -> locationUpdated(dto);
                case DRIVER_BROADCAST -> broadcastDrivers(dto);
                case RIDE_OFFER_BROADCAST -> offerBroadcast(dto);
                case RIDE_OFFER_WITHDRAWN -> offerWithdrawn(dto);
                case RIDE_MATCH_FAILED ->
                        relayRideOnce("MATCH_FAILED", dto, () -> matchFailed(dto));
                default -> log.warn("Unknown topic: {}", topic);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {

            log.error("Error while consuming message", e);
            throw new IllegalStateException("Websocket service Kafka listener failed", e);

        }
    }

    private void relayRideOnce(String status, Object event, Runnable relay) {
        if (!(event instanceof Map<?, ?> fields) || fields.get("rideId") == null) {
            relay.run();
            return;
        }
        String key = status + ":" + fields.get("rideId");
        long now = System.currentTimeMillis();
        if (relayedRideEvents.putIfAbsent(key, now) != null) return;
        try {
            relay.run();
        } catch (RuntimeException ex) {
            relayedRideEvents.remove(key, now);
            throw ex;
        }
        if (relayedRideEvents.size() > 10_000) {
            long expiry = now - 300_000;
            relayedRideEvents.entrySet().removeIf(entry -> entry.getValue() < expiry);
        }
    }


    private void rideRequest(Object event) {
        RideRequestedEvent data = objectMapper.convertValue(event, RideRequestedEvent.class);
        log.info("Under ride request and data is {}", data.getRideId());
        realtimeEventService.publishRideUpdate(data);
    }

    private void rideAssigned(Object event) {
        RideAcceptedEvent data = objectMapper.convertValue(event, RideAcceptedEvent.class);
        log.info("Under ride assigned and data is {}", data.getRideId());
        if (driverRideLightweightService != null) {
            driverRideLightweightService.putDriverHasActiveRide(
                    data.getDriverId(),
                    data.getRideId(),
                    RideStatus.DRIVER_ASSIGNED,
                    data.getPickupLatitude(),
                    data.getPickupLongitude(),
                    data.getDestinationLatitude(),
                    data.getDestinationLongitude(),
                    data.getRiderId());
        }
        realtimeEventService.publishRideUpdate(data);
    }

    private void rideArrived(Object event) {
        DriverArrivedEvent data = objectMapper.convertValue(event, DriverArrivedEvent.class);
        log.info("Under ride arrived and data is {}", data.getRideId());
        if (driverRideLightweightService != null) {
            driverRideLightweightService.updateStatus(data.getDriverId(), RideStatus.DRIVER_ARRIVED);
        }
        realtimeEventService.publishRideUpdate(data);
    }

    private void rideStarted(Object event) {
        RideStartedEvent data = objectMapper.convertValue(event, RideStartedEvent.class);
        log.info("Under ride started and data is {}", data.getRideId());
        if (driverRideLightweightService != null) {
            driverRideLightweightService.updateStatus(data.getDriverId(), RideStatus.STARTED);
        }
        realtimeEventService.publishRideUpdate(data);
    }

    private void rideCompleted(Object event) {
        RideCompletedEvent data = objectMapper.convertValue(event, RideCompletedEvent.class);
        log.info("Under ride completed and data is {}", data.getRideId());
        if (driverRideLightweightService != null) {
            driverRideLightweightService.clear(data.getDriverId(), data.getRideId());
        }
        realtimeEventService.publishRideUpdate(data);
    }

    private void rideCancelled(Object event) {
        RideCancelledEvent data = objectMapper.convertValue(event, RideCancelledEvent.class);
        log.info("Under ride cancelled and data is {}", data.getRideId());
        if (driverRideLightweightService != null) {
            driverRideLightweightService.clear(data.getDriverId(), data.getRideId());
        }
        realtimeEventService.publishRideUpdate(data);
    }

    private void locationUpdated(Object event) {
        LocationUpdateEvent data = objectMapper.convertValue(event, LocationUpdateEvent.class);
        if (data.getEventId() != null && relayedLocationEvents.putIfAbsent(
                data.getEventId(), System.currentTimeMillis()) != null) {
            return;
        }
        if (relayedLocationEvents.size() > 10_000) {
            long expiry = System.currentTimeMillis() - 60_000;
            relayedLocationEvents.entrySet().removeIf(entry -> entry.getValue() < expiry);
        }
        log.info("Under location update and ride id is {}", data.getRideId());
        realtimeEventService.publishRideUpdate(data);
    }

    /**
     * One matching broadcast, fanned out: a targeted offer push per
     * driver plus one search-progress push for the rider.
     */
    private void offerBroadcast(Object event) {
        RideOfferBroadcastPayload broadcast =
                objectMapper.convertValue(event, RideOfferBroadcastPayload.class);
        if (broadcast.getRideId() == null || broadcast.getTargets() == null) {
            log.warn("Ignoring ride.offer.broadcast without rideId/targets");
            return;
        }
        for (RideOfferBroadcastPayload.OfferTarget target : broadcast.getTargets()) {
            if (target.getDriverId() == null) {
                continue;
            }
            RideOfferEvent offer = RideOfferEvent.builder()
                    .rideId(broadcast.getRideId())
                    .driverId(target.getDriverId())
                    .rideStatus(RideStatus.OFFER_SENT)
                    .pickupLatitude(broadcast.getPickupLatitude())
                    .pickupLongitude(broadcast.getPickupLongitude())
                    .destinationLatitude(broadcast.getDestinationLatitude())
                    .destinationLongitude(broadcast.getDestinationLongitude())
                    .estimatedDistance(broadcast.getEstimatedDistance())
                    .estimatedDuration(broadcast.getEstimatedDuration())
                    .totalFare(broadcast.getTotalFare())
                    .currency(broadcast.getCurrency())
                    .rideTypeCode(broadcast.getVehicleType())
                    .distanceKm(target.getDistanceKm())
                    .attempt(broadcast.getAttempt())
                    .radiusKm(broadcast.getRadiusKm())
                    .expiresAt(broadcast.getExpiresAt())
                    .eventTime(LocalDateTime.now())
                    .build();
            realtimeEventService.publishRideUpdate(offer);
        }
        RideSearchProgressEvent progress = RideSearchProgressEvent.builder()
                .rideId(broadcast.getRideId())
                .riderId(broadcast.getRiderId())
                .rideStatus(RideStatus.SEARCHING)
                .attempt(broadcast.getAttempt())
                .radiusKm(broadcast.getRadiusKm())
                .driverCount(broadcast.getTargets().size())
                .eventTime(LocalDateTime.now())
                .build();
        realtimeEventService.publishRideUpdate(progress);
        log.info("Fanned out ride {} broadcast to {} drivers (attempt {}, {} km)",
                broadcast.getRideId(), broadcast.getTargets().size(),
                broadcast.getAttempt(), broadcast.getRadiusKm());
    }

    private void offerWithdrawn(Object event) {
        RideOfferWithdrawnPayload withdrawn =
                objectMapper.convertValue(event, RideOfferWithdrawnPayload.class);
        if (withdrawn.getRideId() == null || withdrawn.getDriverIds() == null) {
            log.warn("Ignoring ride.offer.withdrawn without rideId/driverIds");
            return;
        }
        for (Long driverId : withdrawn.getDriverIds()) {
            RideOfferWithdrawnEvent push = RideOfferWithdrawnEvent.builder()
                    .rideId(withdrawn.getRideId())
                    .driverId(driverId)
                    .rideStatus(RideStatus.OFFER_WITHDRAWN)
                    .reason(withdrawn.getReason())
                    .eventTime(LocalDateTime.now())
                    .build();
            realtimeEventService.publishRideUpdate(push);
        }
        log.info("Fanned out ride {} withdrawal to {} drivers ({})",
                withdrawn.getRideId(), withdrawn.getDriverIds().size(),
                withdrawn.getReason());
    }

    private void matchFailed(Object event) {
        RideMatchFailedEvent failed = objectMapper.convertValue(event, RideMatchFailedEvent.class);
        if (failed.getRideId() == null || failed.getRiderId() == null) {
            throw new IllegalArgumentException("ride.match.failed missing rideId or riderId");
        }
        failed.setRideStatus(RideStatus.MATCH_FAILED);
        failed.setMessage("NO_AVAILABLE_DRIVER".equals(failed.getReason())
                ? "No drivers are available right now. Please try again."
                : "Driver search ended. Please try again.");
        failed.setEventTime(LocalDateTime.now());
        realtimeEventService.publishRideUpdate(failed);
    }

    private void broadcastDrivers(Object data) {
        log.info("Under the broadcast drivers .");
        List<NearbyDriver> allDrivers = objectMapper.convertValue(
                data,
                new TypeReference<List<NearbyDriver>>() { }
        );
        log.debug("Relaying {} nearby driver positions", allDrivers.size());
        realtimeEventService.publishDriverUpdates(allDrivers);
    }
}
