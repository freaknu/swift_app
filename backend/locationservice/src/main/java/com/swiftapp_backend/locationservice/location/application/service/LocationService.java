package com.swiftapp_backend.locationservice.location.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.locationservice.location.api.dto.driver.NearbyDriverDto;
import com.swiftapp_backend.locationservice.location.api.dto.event.LocationUpdateDto;
import com.swiftapp_backend.locationservice.location.application.mapper.LocationUpdateMapper;
import com.swiftapp_backend.locationservice.location.application.port.LocationCachePort;
import com.swiftapp_backend.locationservice.location.application.port.LocationEventPublisher;
import com.swiftapp_backend.locationservice.location.domain.events.LocationEventUpdate;
import com.swiftapp_backend.locationservice.location.domain.model.Location;
import com.swiftapp_backend.locationservice.location.domain.repository.LocationRepository;
import com.swiftapp_backend.locationservice.location.application.port.LocationEventTopics;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {
    private static final String RIDE_LOCATION_KEY_PREFIX = "rideLocation:";
    private static final String DRIVER_LOCATION_KEY_PREFIX = "driverLocation:";
    private static final String AVAILABLE_DRIVERS_GEO_KEY = "availableDrivers";
    private static final Duration AVAILABLE_POSITION_MAX_AGE = Duration.ofSeconds(60);

    private final LocationCachePort locationCache;
    private final ObjectMapper objectMapper;
    private final LocationUpdateMapper locationUpdateMapper;
    private final LocationRepository locationRepository;
    private final LocationPersistenceService locationPersistenceService;
    private final LocationEventPublisher locationEventPublisher;
    private final DriverPresenceClient driverPresenceClient;
    private final RideLocationAccessClient rideAccessClient;

    /** Called from the authenticated HTTP endpoint. Client supplied status and
     * vehicle type are ignored; the driver service supplies both. */
    public List<LocationUpdateDto> addDriverLocation(LocationUpdateDto data, Long authUserId) {
        if (data.getDriverId() == null || authUserId == null) {
            throw new IllegalArgumentException("Driver identity is required");
        }
        DriverPresenceClient.Presence presence = driverPresenceClient.get(authUserId, data.getDriverId());
        if (data.getRideId() != null) {
            RideLocationAccessClient.Ride ride = rideAccessClient.assignedToDriver(
                    data.getRideId(), data.getDriverId());
            data.setRiderId(ride.riderId());
            enrichRideLocation(data, ride);
        } else {
            data.setRiderId(null);
        }
        data.setDriverStatus(presence.driverStatus());
        data.setVehicleType(presence.vehicleTypeEnum());
        data.setEventId(UUID.randomUUID().toString());
        data.setEventTime(Instant.now());
        List<LocationUpdateDto> saved = addLocation(data, presence);
        if (data.getRideId() != null) {
            locationEventPublisher.publishConfirmed("location-updated-topic",
                    "ride-location-" + data.getRideId(), data);
        }
        return saved;
    }

    @Transactional
    public List<LocationUpdateDto> addLocation(LocationUpdateDto data) {
        return addLocation(data, null);
    }

    private List<LocationUpdateDto> addLocation(LocationUpdateDto data,
                                                DriverPresenceClient.Presence presence) {
        validateLocation(data);
        if (data.getEventId() == null || data.getEventId().isBlank()) {
            data.setEventId(UUID.randomUUID().toString());
        }
        if (data.getEventTime() == null) data.setEventTime(Instant.now());
        String key = data.getRideId() == null
                ? driverLocationKey(data.getDriverId()) : rideLocationKey(data.getRideId());
        locationCache.append(key,data);
        updateAvailableDriverIndex(data, presence);
        locationPersistenceService.save(data);
        if (presence != null) sendLocation(data, presence);
        return objectMapper
                .convertValue(locationCache.getList(key),new TypeReference<List<LocationUpdateDto>>(){});
    }
    private void sendLocation(LocationUpdateDto data, DriverPresenceClient.Presence presence) {
        LocationEventUpdate event = LocationEventUpdate
                .builder()
                .driverId(data.getDriverId())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .driverStatus(data.getRideId() == null ? presence.driverStatus() : "INACTIVE")
                .vehicleTypeEnum(presence.vehicleTypeEnum())
                .build();
        locationEventPublisher.publish(LocationEventTopics.DRIVER_LOCATION,event.getDriverId().toString(),event);
    }

    public List<LocationUpdateDto> getRecentLocation(Long rideId,Long driverId) {
        String key = rideId == null ? driverLocationKey(driverId) : rideLocationKey(rideId);
        if(!locationCache.exists(key)) {
            return new ArrayList<>();
        }
        return objectMapper
                .convertValue(locationCache.getList(key),new TypeReference<List<LocationUpdateDto>>(){});
    }

    public List<LocationUpdateDto> getLocationHistory(Long rideId,Long driverId) {
        List<Location> locations = locationRepository.findAllByRideIdAndDriverId(rideId,driverId);
        return locations
                .stream()
                .map(locationUpdateMapper::toDto)
                .toList();
    }

    public LocationUpdateDto getDriverPosition(Long rideId, Long authUserId) {
        RideLocationAccessClient.Ride ride = rideAccessClient.participant(rideId, null, authUserId);
        if (ride.driverId() == null) return null;
        LocationUpdateDto cached = getLatestLocation(rideLocationKey(rideId));
        if (cached != null && ride.driverId().equals(cached.getDriverId())
                && rideId.equals(cached.getRideId())) return cached;
        return locationRepository.findLatestByRideIdAndDriverId(rideId, ride.driverId())
                .map(locationUpdateMapper::toDto).orElse(null);
    }

    public void requireRideParticipant(Long rideId, Long driverId, Long authUserId) {
        rideAccessClient.participant(rideId, driverId, authUserId);
    }


    public void completeRide(Long rideId) {
        String key = rideLocationKey(rideId);
        locationCache.delete(key);
    }

    public void completeRide(Long rideId, Long authUserId) {
        RideLocationAccessClient.Ride ride = rideAccessClient.participant(rideId, null, authUserId);
        if (!Set.of("COMPLETED", "CANCELLED", "MATCH_FAILED").contains(ride.status())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Ride is still active");
        }
        completeRide(rideId);
    }

    public void driverBecameUnavailable(Long driverId) {
        if (driverId != null) {
            locationCache.removeGeoLocation(AVAILABLE_DRIVERS_GEO_KEY, driverId.toString());
        }
    }

    public List<NearbyDriverDto> getAllDrivers(double latitude,double longitude) {
        return getAllDrivers(latitude, longitude, 5.0);
    }

    public List<NearbyDriverDto> getAllDrivers(double latitude,double longitude,double distance) {
        return getAllDrivers(latitude, longitude, distance, null);
    }

    public List<NearbyDriverDto> getAllDrivers(double latitude,double longitude,double distance,
                                               String vehicleType) {
        validateDriverSearch(latitude, longitude, distance);

        List<NearbyDriverDto> nearbyDrivers = new ArrayList<>();
        Set<Long> addedDriverIds = new HashSet<>();

        for (String member :
                locationCache.findGeoMembersWithin(
                        AVAILABLE_DRIVERS_GEO_KEY,
                        latitude,
                        longitude,
                        distance
                )) {

            Long driverId = extractDriverId(member);
            if (driverId == null || !addedDriverIds.add(driverId)) {
                continue;
            }

            LocationUpdateDto location = getLatestLocation(driverLocationKey(driverId));
            if (!isAvailableDriverLocation(driverId, location)) {
                locationCache.removeGeoLocation(
                        AVAILABLE_DRIVERS_GEO_KEY,
                        driverId.toString()
                );
                continue;
            }

            if (vehicleType != null && !vehicleType.isBlank()
                    && !vehicleType.equals(location.getVehicleType())) continue;

            nearbyDrivers.add(toNearbyDriver(location));
        }

        return nearbyDrivers;
    }

    private LocationUpdateDto getLatestLocation(String key) {
        List<Object> locations = locationCache.getList(key);
        if (locations.isEmpty()) {
            return null;
        }

        Object latestLocation = locations.get(locations.size() - 1);
        return objectMapper.convertValue(latestLocation, LocationUpdateDto.class);
    }

    private NearbyDriverDto toNearbyDriver(LocationUpdateDto location) {
        return new NearbyDriverDto(
                location.getDriverId(),
                location.getNearByPlace(),
                location.getLatitude(),
                location.getLongitude(),
                location.getVehicleType(),
                location.getEventTime()
        );
    }

    private void updateAvailableDriverIndex(LocationUpdateDto data,
                                            DriverPresenceClient.Presence presence) {
        if (data.getDriverId() == null) {
            return;
        }

        String driverId = data.getDriverId().toString();
        if (data.getRideId() == null
                && (presence == null || "ACTIVE".equals(presence.driverStatus()))) {
            locationCache.addGeoLocation(
                    AVAILABLE_DRIVERS_GEO_KEY,
                    driverId,
                    data.getLatitude(),
                    data.getLongitude()
            );
            return;
        }

        locationCache.removeGeoLocation(AVAILABLE_DRIVERS_GEO_KEY, driverId);
    }

    private boolean isAvailableDriverLocation(
            Long driverId,
            LocationUpdateDto location
    ) {
        return location != null
                && driverId.equals(location.getDriverId())
                && location.getRideId() == null
                && "ACTIVE".equals(location.getDriverStatus())
                && location.getVehicleType() != null
                && location.getEventTime() != null
                && !location.getEventTime().isBefore(Instant.now().minus(AVAILABLE_POSITION_MAX_AGE));
    }

    private Long extractDriverId(Object member) {
        if (member instanceof Number number) {
            return number.longValue();
        }
        if (member instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String driverLocationKey(Long driverId) {
        return DRIVER_LOCATION_KEY_PREFIX + driverId;
    }

    private String rideLocationKey(Long rideId) {
        return RIDE_LOCATION_KEY_PREFIX + rideId;
    }

    private void validateDriverSearch(
            double latitude,
            double longitude,
            double distance
    ) {

        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        if (!Double.isFinite(distance) || distance < 0) {
            throw new IllegalArgumentException("Distance must be greater than or equal to 0");
        }
    }

    private void validateLocation(LocationUpdateDto data) {
        if (data == null || data.getDriverId() == null || data.getDriverId() <= 0) {
            throw new IllegalArgumentException("Driver id is required");
        }
        if (!Double.isFinite(data.getLatitude()) || data.getLatitude() < -90
                || data.getLatitude() > 90 || !Double.isFinite(data.getLongitude())
                || data.getLongitude() < -180 || data.getLongitude() > 180) {
            throw new IllegalArgumentException("Location coordinates are invalid");
        }
    }

    private void enrichRideLocation(LocationUpdateDto data, RideLocationAccessClient.Ride ride) {
        if (ride.pickupLatitude() != null && ride.pickupLongitude() != null) {
            double distance = distanceMeters(data.getLatitude(), data.getLongitude(),
                    ride.pickupLatitude(), ride.pickupLongitude());
            data.setDistanceToPickupMeters(round(distance));
            data.setEtaMinutes("DRIVER_ARRIVED".equals(ride.status()) ? 0 : etaMinutes(distance));
            data.setDriverArrived("DRIVER_ARRIVED".equals(ride.status()));
        }
        if (ride.destinationLatitude() != null && ride.destinationLongitude() != null) {
            double distance = distanceMeters(data.getLatitude(), data.getLongitude(),
                    ride.destinationLatitude(), ride.destinationLongitude());
            data.setDistanceToDestinationMeters(round(distance));
            data.setDestinationEtaMinutes(etaMinutes(distance));
        }
    }

    private double distanceMeters(double latitude1, double longitude1,
                                  double latitude2, double longitude2) {
        double lat = Math.toRadians(latitude2 - latitude1);
        double lon = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(lat / 2) * Math.sin(lat / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(lon / 2) * Math.sin(lon / 2);
        return 6_371_000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private long etaMinutes(double distanceMeters) {
        return Math.max(1L, (long) Math.ceil((distanceMeters / 1000.0) / 30.0 * 60.0));
    }

}
