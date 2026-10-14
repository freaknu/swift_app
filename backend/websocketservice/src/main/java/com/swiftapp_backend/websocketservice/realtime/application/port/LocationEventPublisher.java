package com.swiftapp_backend.websocketservice.realtime.application.port;

import com.swiftapp_backend.websocketservice.realtime.domain.event.LocationUpdateEvent;

public interface LocationEventPublisher {
    void publishLocationUpdate(LocationUpdateEvent event);
}
