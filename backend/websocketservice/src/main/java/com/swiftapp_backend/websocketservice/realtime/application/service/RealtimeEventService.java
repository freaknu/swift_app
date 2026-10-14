package com.swiftapp_backend.websocketservice.realtime.application.service;

import com.swiftapp_backend.websocketservice.realtime.application.port.RealtimeMessagePublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RealtimeEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.model.NearbyDriver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeEventService {
    private final RealtimeMessagePublisher messagePublisher;

    public void publishRideUpdate(RealtimeEvent event) {
        messagePublisher.publishRideUpdate(event);
    }

    public void publishDriverUpdates(List<NearbyDriver> drivers) {
        drivers.forEach(messagePublisher::publishDriverUpdate);
    }
}
