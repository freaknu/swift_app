package com.swiftapp_backend.websocketservice.realtime.application.port;

import com.swiftapp_backend.websocketservice.realtime.domain.event.MessageCreatedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RealtimeEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.model.NearbyDriver;

public interface RealtimeMessagePublisher {
    void publishRideUpdate(RealtimeEvent event);

    void publishDriverUpdate(NearbyDriver driver);

    void publishChatMessage(MessageCreatedEvent event);
}
