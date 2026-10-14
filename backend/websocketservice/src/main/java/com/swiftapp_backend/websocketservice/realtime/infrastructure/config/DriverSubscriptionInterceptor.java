package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Stops another socket client from subscribing to a driver's offers. */
@Component
@RequiredArgsConstructor
public class DriverSubscriptionInterceptor implements ChannelInterceptor {
    private static final String DRIVER_TOPIC_PREFIX = "/topic/drivers/";
    private static final String RIDER_TOPIC_PREFIX = "/topic/riders/";
    private static final String RIDE_TOPIC_PREFIX = "/topic/rides/";
    private static final String CHAT_TOPIC_PREFIX = "/topic/chat/session/";
    public static final String AUTH_USER_ID_ATTRIBUTE = "swift.authUserId";

    private final JwtDecoder websocketJwtDecoder;
    private final DriverIdentityClient identityClient;
    private final RiderIdentityClient riderIdentityClient;
    private final RideSubscriptionAccessClient rideAccessClient;
    private final ChatSessionAccessClient chatSessionAccessClient;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message,
                StompHeaderAccessor.class);
        if (accessor == null) return message;
        if (accessor.getCommand() == StompCommand.CONNECT) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new MessagingException("WebSocket authorization required");
            }
            {
                try {
                    Jwt jwt = websocketJwtDecoder.decode(authorization.substring(7));
                    Object userIdClaim = jwt.getClaim("userId");
                    if (userIdClaim == null) throw new MessagingException("Missing userId claim");
                    Long userId = Long.valueOf(userIdClaim.toString());
                    if (userId <= 0) throw new MessagingException("Invalid userId claim");
                    Map<String, Object> session = accessor.getSessionAttributes();
                    if (session == null) throw new MessagingException("Missing socket session");
                    session.put(AUTH_USER_ID_ATTRIBUTE, userId);
                } catch (Exception ex) {
                    throw new MessagingException("Invalid WebSocket authorization", ex);
                }
            }
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE
                && accessor.getDestination() != null
                && accessor.getDestination().startsWith(RIDER_TOPIC_PREFIX)) {
            Long riderId = topicId(accessor.getDestination(), RIDER_TOPIC_PREFIX);
            Long userId = sessionUserId(accessor);
            if (!riderIdentityClient.belongsTo(userId, riderId)) {
                throw new MessagingException("Rider subscription is not owned by this user");
            }
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE
                && accessor.getDestination() != null
                && accessor.getDestination().startsWith(RIDE_TOPIC_PREFIX)) {
            Long rideId = topicId(accessor.getDestination(), RIDE_TOPIC_PREFIX);
            Long userId = sessionUserId(accessor);
            if (!rideAccessClient.belongsTo(userId, rideId)) {
                throw new MessagingException("Ride subscription is not owned by this user");
            }
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE
                && accessor.getDestination() != null
                && accessor.getDestination().startsWith(DRIVER_TOPIC_PREFIX)) {
            String suffix = accessor.getDestination().substring(DRIVER_TOPIC_PREFIX.length());
            Long driverId;
            try {
                driverId = Long.valueOf(suffix);
            } catch (NumberFormatException ex) {
                throw new MessagingException("Invalid driver subscription");
            }
            Map<String, Object> session = accessor.getSessionAttributes();
            Object rawUserId = session == null ? null : session.get(AUTH_USER_ID_ATTRIBUTE);
            if (!(rawUserId instanceof Long userId)) {
                throw new MessagingException("Authorization required for driver subscription");
            }
            try {
                if (!identityClient.belongsTo(userId, driverId)) {
                    throw new MessagingException("Driver subscription is not owned by this user");
                }
            } catch (MessagingException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new MessagingException("Could not verify driver subscription", ex);
            }
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE
                && accessor.getDestination() != null
                && accessor.getDestination().startsWith(CHAT_TOPIC_PREFIX)) {
            String suffix = accessor.getDestination().substring(CHAT_TOPIC_PREFIX.length());
            Long sessionId;
            try {
                sessionId = Long.valueOf(suffix);
            } catch (NumberFormatException ex) {
                throw new MessagingException("Invalid chat session subscription");
            }
            Map<String, Object> session = accessor.getSessionAttributes();
            Object rawUserId = session == null ? null : session.get(AUTH_USER_ID_ATTRIBUTE);
            if (!(rawUserId instanceof Long userId)) {
                throw new MessagingException("Authorization required for chat subscription");
            }
            if (!chatSessionAccessClient.belongsTo(userId, sessionId)) {
                throw new MessagingException("Chat subscription is not owned by this user");
            }
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE
                && accessor.getDestination() != null) {
            String destination = accessor.getDestination();
            if (!(destination.startsWith(DRIVER_TOPIC_PREFIX)
                    || destination.startsWith(RIDER_TOPIC_PREFIX)
                    || destination.startsWith(RIDE_TOPIC_PREFIX)
                    || destination.startsWith(CHAT_TOPIC_PREFIX)
                    || destination.startsWith("/user/queue/"))) {
                throw new MessagingException("Unsupported subscription");
            }
            sessionUserId(accessor);
        }
        if (accessor.getCommand() == StompCommand.SEND && accessor.getDestination() != null) {
            String destination = accessor.getDestination();
            if (destination.startsWith("/topic/") || destination.startsWith("/queue/")) {
                throw new MessagingException("Clients cannot publish directly to broker topics");
            }
            if (destination.startsWith("/app/chat.")) {
                if (!destination.equals("/app/chat.send") && !destination.equals("/app/chat.reply")) {
                    throw new MessagingException("Unsupported legacy chat mutation");
                }
                Map<String, Object> session = accessor.getSessionAttributes();
                Object identity = session == null ? null : session.get(AUTH_USER_ID_ATTRIBUTE);
                if (!(identity instanceof Long userId) || userId <= 0) {
                    throw new MessagingException("Authorization required for chat write");
                }
            }
            if (destination.equals("/app/location")) {
                sessionUserId(accessor);
            } else if (!destination.equals("/app/chat.send")
                    && !destination.equals("/app/chat.reply")) {
                throw new MessagingException("Unsupported client message destination");
            }
        }
        return message;
    }

    private Long topicId(String destination, String prefix) {
        try {
            long id = Long.parseLong(destination.substring(prefix.length()));
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // Fall through to the same rejection for malformed IDs.
        }
        throw new MessagingException("Invalid subscription destination");
    }

    private Long sessionUserId(StompHeaderAccessor accessor) {
        Map<String, Object> session = accessor.getSessionAttributes();
        Object value = session == null ? null : session.get(AUTH_USER_ID_ATTRIBUTE);
        if (value instanceof Long id && id > 0) return id;
        throw new MessagingException("WebSocket authorization required");
    }
}
