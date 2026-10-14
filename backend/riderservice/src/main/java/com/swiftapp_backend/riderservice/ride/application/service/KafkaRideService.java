package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.application.port.MessagePublisherPort;
import com.swiftapp_backend.riderservice.ride.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaRideService {

    private static final String RIDE_REQUESTED_TOPIC = "ride-requested";
    private static final String RIDE_ASSIGNED_TOPIC = "ride-assigned";
    private static final String DRIVER_ARRIVED_TOPIC = "driver-arrived";
    private static final String RIDE_STARTED_TOPIC = "ride-started";
    private static final String RIDE_COMPLETED_TOPIC = "ride-completed";
    private static final String RIDE_CANCELLED_TOPIC = "ride-cancelled";
    private static final String RIDE_HISTORY_CREATE_TOPIC = "ride-history-created-topic";
    private static final String APPLY_OFFER_CREATE_TOPIC = "ride-offer-applied-topic";
    private static final String RIDE_CREATE_FETCH_USER_DRIVER_TOPIC = "ride-create-driver-user-topic";
    private final MessagePublisherPort producer;

    public void publishRideHistoryCreate(RideHistoryCreateEvent event) {
        producer.publish(RIDE_HISTORY_CREATE_TOPIC,"ride" + event.getRideId(),event);
    }
    @Async
    public void publishRideRequested(RideRequestedEventDto event) {
        producer.publish(RIDE_REQUESTED_TOPIC,
                event.getRideId().toString(),
                event);
        producer.publish("ride.requested",
                event.getRideId().toString(),
                event);
    }
    @Async
    public void publishRideAssigned(RideAcceptedEventDto event) {
        producer.publish(RIDE_ASSIGNED_TOPIC,
                event.getRideId().toString(),
                event);
        producer.publish("ride.assigned",
                event.getRideId().toString(),
                event);
        producer.publish("driver.assigned",
                event.getRideId().toString(),
                event);
    }
    @Async
    public void publishDriverArrived(DriverArrivedEventDto event) {
        producer.publish(DRIVER_ARRIVED_TOPIC,
                event.getRideId().toString(),
                event);
        producer.publish("driver.arrived",
                event.getRideId().toString(),
                event);
    }
    @Async
    public void publishRideStarted(RideStartedEventDto event) {
        producer.publish(RIDE_STARTED_TOPIC,
                event.getRideId().toString(),
                event);
        producer.publish("ride.started",
                event.getRideId().toString(),
                event);
    }
    public void publishRideCompleted(RideCompletedEventDto event) {
        // Queue in the ride transaction: consumers must see only committed rides.
        // The production publisher persists these two topics in a retrying outbox.
        producer.publish(RIDE_COMPLETED_TOPIC,
                event.getRideId().toString(),
                event);
        producer.publish("ride.completed",
                event.getRideId().toString(),
                event);
    }
    @Async
    public void publishRideCancelled(RideCancelledEventDto event) {
        producer.publish(RIDE_CANCELLED_TOPIC,
                event.getRideId().toString(),
                event);
        producer.publish("ride.cancelled",
                event.getRideId().toString(),
                event);
    }

    @Async
    public void publishApplyOfferCreate(ApplyOfferRequestEvent event) {
        producer.publish(APPLY_OFFER_CREATE_TOPIC,"applyOffer" + event.getAuthUserId(),event);
    }

    @Async
    public void publishRideCreateFetchDriverUserDetail(RideDriverUserFetchRequestEvent event) {
        producer.publish(RIDE_CREATE_FETCH_USER_DRIVER_TOPIC,"rideDriverUserCreateRequest"+event.getRideId(),event);
    }
}
