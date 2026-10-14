package com.swiftapp_backend.riderservice.ride.application.port;

import com.swiftapp_backend.riderservice.ride.domain.event.RideEvent;

public interface RideEventPublisherPort {
    void publishAfterCommit(RideEvent event);
}
