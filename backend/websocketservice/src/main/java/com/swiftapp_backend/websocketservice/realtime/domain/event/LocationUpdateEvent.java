package com.swiftapp_backend.websocketservice.realtime.domain.event;

import com.swiftapp_backend.websocketservice.realtime.domain.enums.Direction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateEvent implements RealtimeEvent {
    /** Correlates the WebSocket -> Kafka message with the persisted Kafka relay. */
    private String eventId;
    private Instant eventTime;
    private Long id;
    private Long driverId;
    private Long rideId;
    private Long riderId;
    private double latitude;
    private double longitude;
    private String nearByPlace;
    private Direction direction;
    private Double distanceToPickupMeters;
    private Long etaMinutes;
    private Boolean driverArrived;
    private Double distanceToDestinationMeters;
    private Long destinationEtaMinutes;
}
