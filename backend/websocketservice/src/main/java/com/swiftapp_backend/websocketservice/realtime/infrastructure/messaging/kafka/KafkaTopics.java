package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

public final class KafkaTopics {
    public static final String RIDE_REQUESTED = "ride-requested";
    public static final String RIDE_ASSIGNED = "ride-assigned";
    public static final String DRIVER_ARRIVED = "driver-arrived";
    public static final String RIDE_STARTED = "ride-started";
    public static final String RIDE_COMPLETED = "ride-completed";
    public static final String RIDE_CANCELLED = "ride-cancelled";
    public static final String LOCATION_UPDATE = "location-update-topic";
    public static final String LOCATION_UPDATED = "location-updated-topic";
    /** Internal event emitted by WebSocket arrival detection and consumed by rider-service. */
    public static final String DRIVER_ARRIVAL_DETECTED = "driver.arrival.detected";
    public static final String DRIVER_BROADCAST = "driver-broadcast-topic";
    public static final String RIDE_OFFER_BROADCAST = "ride.offer.broadcast";
    public static final String RIDE_OFFER_WITHDRAWN = "ride.offer.withdrawn";
    public static final String DRIVER_MATCHED = "driver.matched";
    public static final String RIDE_MATCH_FAILED = "ride.match.failed";

    public static final String RIDE_REQUESTED_DOT = "ride.requested";
    public static final String RIDE_ASSIGNED_DOT = "ride.assigned";
    public static final String DRIVER_ASSIGNED_DOT = "driver.assigned";
    public static final String DRIVER_ARRIVED_DOT = "driver.arrived";
    public static final String RIDE_STARTED_DOT = "ride.started";
    public static final String RIDE_COMPLETED_DOT = "ride.completed";
    public static final String RIDE_CANCELLED_DOT = "ride.cancelled";

    public static final String PAYMENT_ORDER_CREATED = "payment.order.created";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";

    //Chat topics ->

    public static final String MESSAGE_CREATED = "message.created";
    public static final String MESSAGE_UPDATED = "message.updated";
    public static final String MESSAGE_DELETED = "message.deleted";
    public static final String MESSAGE_REPLY = "message.reply";
    public static final String CHAT_SESSION_CREATED = "chatsession.created";
    public static final String NATIVE_MESSAGE_CREATED = "chat.message.created";



    private KafkaTopics() {
    }
}
