package com.swiftapp_backend.websocketservice.realtime.application.service;

import com.swiftapp_backend.websocketservice.realtime.application.mapper.LocationUpdateMapper;
import com.swiftapp_backend.websocketservice.realtime.application.port.LocationEventPublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.enums.RideStatus;
import com.swiftapp_backend.websocketservice.realtime.domain.event.DriverArrivedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.LocationUpdateEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.model.LocationUpdate;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaRideEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;


@Slf4j
@Service
public class LiveLocationService {
    private static final double ARRIVAL_RADIUS_METERS = 100.0;
    private static final double ETA_SPEED_KPH = 30.0;

    private final LocationEventPublisher locationEventPublisher;
    private final LocationUpdateMapper locationUpdateMapper;
    private final DriverRideLightweightService driverRideLightweightService;
    private final KafkaRideEventPublisher kafkaRideEventPublisher;

    public LiveLocationService(LocationEventPublisher locationEventPublisher,
                               LocationUpdateMapper locationUpdateMapper) {
        this(locationEventPublisher, locationUpdateMapper, null, null);
    }

    @Autowired
    public LiveLocationService(LocationEventPublisher locationEventPublisher,
                               LocationUpdateMapper locationUpdateMapper,
                               DriverRideLightweightService driverRideLightweightService,
                               KafkaRideEventPublisher kafkaRideEventPublisher) {
        this.locationEventPublisher = locationEventPublisher;
        this.locationUpdateMapper = locationUpdateMapper;
        this.driverRideLightweightService = driverRideLightweightService;
        this.kafkaRideEventPublisher = kafkaRideEventPublisher;
    }

    public void consumeLocation(LocationUpdate data) {
        validate(data);

        if (driverRideLightweightService != null
                && !driverRideLightweightService.acceptsLocation(data.getDriverId(), data.getRideId())) {
            log.warn("Ignoring location for unassigned ride {} from driver {}",
                    data.getRideId(), data.getDriverId());
            return;
        }

        LocationUpdateEvent event = locationUpdateMapper.toEvent(data);
        event.setEventId(UUID.randomUUID().toString());
        event.setEventTime(Instant.now());

        if (driverRideLightweightService != null) {
            enrichAndDetectArrival(data, event);
        }

        locationEventPublisher.publishLocationUpdate(event);
    }

    private void enrichAndDetectArrival(LocationUpdate data, LocationUpdateEvent event) {
        Long rideId = data.getRideId();
        Long riderId = driverRideLightweightService.getRiderId(rideId);
        if (riderId != null) event.setRiderId(riderId);

        Double destinationLatitude = driverRideLightweightService.getDestinationLatitude(rideId);
        Double destinationLongitude = driverRideLightweightService.getDestinationLongitude(rideId);
        if (destinationLatitude != null && destinationLongitude != null) {
            double destinationDistance = distanceMeters(
                    data.getLatitude(), data.getLongitude(),
                    destinationLatitude, destinationLongitude);
            event.setDistanceToDestinationMeters(round(destinationDistance));
            event.setDestinationEtaMinutes(etaMinutes(destinationDistance));
        }

        Double pickupLatitude = driverRideLightweightService.getPickupLatitude(rideId);
        Double pickupLongitude = driverRideLightweightService.getPickupLongitude(rideId);
        if (pickupLatitude == null || pickupLongitude == null) {
            return;
        }

        double distanceMeters = distanceMeters(
                data.getLatitude(), data.getLongitude(), pickupLatitude, pickupLongitude);
        boolean arrived = distanceMeters <= ARRIVAL_RADIUS_METERS
                || driverRideLightweightService.getDriverHasActiveRide(data.getDriverId())
                == RideStatus.DRIVER_ARRIVED;
        event.setDistanceToPickupMeters(round(distanceMeters));
        event.setEtaMinutes(arrived ? 0L : etaMinutes(distanceMeters));
        event.setDriverArrived(arrived);

        RideStatus currentStatus = driverRideLightweightService
                .getDriverHasActiveRide(data.getDriverId());
        if (distanceMeters <= ARRIVAL_RADIUS_METERS
                && currentStatus == RideStatus.DRIVER_ASSIGNED
                && driverRideLightweightService.markArrivalPublished(rideId)) {
            driverRideLightweightService.updateStatus(data.getDriverId(), RideStatus.DRIVER_ARRIVED);
            event.setDriverArrived(true);
            event.setEtaMinutes(0L);
            kafkaRideEventPublisher.publishDriverArrivalDetected(DriverArrivedEvent.builder()
                    .rideId(rideId)
                    .riderId(riderId)
                    .driverId(data.getDriverId())
                    .rideStatus(RideStatus.DRIVER_ARRIVED)
                    .arrivedAt(LocalDateTime.now())
                    .eventTime(LocalDateTime.now())
                    .build());
            log.info("Driver {} reached pickup for ride {} ({} m)",
                    data.getDriverId(), rideId, round(distanceMeters));
        }
    }

    private void validate(LocationUpdate data) {
        if (data == null || data.getDriverId() == null || data.getRideId() == null) {
            throw new IllegalArgumentException("A live ride location requires driverId and rideId");
        }
        if (!Double.isFinite(data.getLatitude()) || data.getLatitude() < -90 || data.getLatitude() > 90
                || !Double.isFinite(data.getLongitude()) || data.getLongitude() < -180
                || data.getLongitude() > 180) {
            throw new IllegalArgumentException("Location coordinates are invalid");
        }
    }

    private long etaMinutes(double distanceMeters) {
        double minutes = (distanceMeters / 1000.0) / ETA_SPEED_KPH * 60.0;
        return Math.max(1L, (long) Math.ceil(minutes));
    }

    private double distanceMeters(double latitude1, double longitude1,
                                 double latitude2, double longitude2) {
        double earthRadiusMeters = 6_371_000.0;
        double latDistance = Math.toRadians(latitude2 - latitude1);
        double lonDistance = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        return earthRadiusMeters * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
