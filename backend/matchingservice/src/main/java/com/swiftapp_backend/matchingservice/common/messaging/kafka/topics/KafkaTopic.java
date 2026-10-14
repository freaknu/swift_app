package com.swiftapp_backend.matchingservice.common.messaging.kafka.topics;

public final class KafkaTopic {

    private KafkaTopic() {
    }

    public static final String DRIVER_LOCATION = "driverlocation.v1";
    public static final String REQUEST_RIDE = "riderequest.v1";

    // ---- consumed: published by riderservice when a rider requests a ride
    public static final String RIDE_REQUESTED = "ride.requested";

    // ---- consumed: driver responses to an offer
    public static final String DRIVER_OFFER_ACCEPTED = "driver.offer.accepted";
    public static final String DRIVER_OFFER_REJECTED = "driver.offer.rejected";

    // ---- published: consumed by riderservice ("driver.matched" handler)
    public static final String DRIVER_MATCHED = "driver.matched";

    // ---- published: an offer was sent to a driver (driver app / websocket)
    public static final String RIDE_OFFER_SENT = "ride.offer.sent";

    // ---- published: one matching attempt broadcast to every driver inside
    // the current search radius (websocket fan-out + driver staging)
    public static final String RIDE_OFFER_BROADCAST = "ride.offer.broadcast";

    // ---- published: offers withdrawn for the listed drivers (another driver
    // accepted first, or the offer window expired before reassignment)
    public static final String RIDE_OFFER_WITHDRAWN = "ride.offer.withdrawn";

    // ---- published: no driver could be found for the ride
    public static final String RIDE_MATCH_FAILED = "ride.match.failed";
}
