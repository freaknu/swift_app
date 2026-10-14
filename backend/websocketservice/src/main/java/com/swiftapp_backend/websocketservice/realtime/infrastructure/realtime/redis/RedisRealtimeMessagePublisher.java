package com.swiftapp_backend.websocketservice.realtime.infrastructure.realtime.redis;

import com.swiftapp_backend.websocketservice.realtime.application.port.RealtimeMessagePublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.event.MessageCreatedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RealtimeEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.model.NearbyDriver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisRealtimeMessagePublisher implements RealtimeMessagePublisher {
    private static final String RIDE_UPDATES_CHANNEL = "ride-updates";
    private static final String DRIVER_UPDATES_CHANNEL = "driver-updates";
    private static final String CHAT_MESSAGES_CHANNEL = "chat-messages";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void publishRideUpdate(RealtimeEvent event) {
        publish(event, RIDE_UPDATES_CHANNEL);
    }

    @Override
    public void publishDriverUpdate(NearbyDriver driver) {
        publish(driver, DRIVER_UPDATES_CHANNEL);
    }

    @Override
    public void publishChatMessage(MessageCreatedEvent event) {
        publish(event, CHAT_MESSAGES_CHANNEL);
    }

    private void publish(Object message, String channel) {
        redisTemplate.convertAndSend(channel, message);
    }
}
