package com.swiftapp_backend.websocketservice.realtime.domain.enums;

public enum RideStatus {

    REQUESTED,

    MATCHING,

    /** The live search is broadcasting to drivers in the current radius. */
    SEARCHING,

    /** The search ended without an available driver; the rider may retry. */
    MATCH_FAILED,

    /** A ride offer pushed to one targeted driver. */
    OFFER_SENT,

    /** A ride offer withdrawn for one driver. */
    OFFER_WITHDRAWN,

    DRIVER_ASSIGNED,

    DRIVER_ARRIVED,

    STARTED,

    COMPLETED,

    CANCELLED
}
