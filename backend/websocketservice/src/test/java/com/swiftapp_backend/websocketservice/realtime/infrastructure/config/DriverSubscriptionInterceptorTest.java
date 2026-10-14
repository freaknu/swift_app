package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverSubscriptionInterceptorTest {
    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final DriverIdentityClient identityClient = mock(DriverIdentityClient.class);
    private final RiderIdentityClient riderIdentityClient = mock(RiderIdentityClient.class);
    private final RideSubscriptionAccessClient rideAccessClient = mock(RideSubscriptionAccessClient.class);
    private final ChatSessionAccessClient chatSessionAccessClient = mock(ChatSessionAccessClient.class);
    private final DriverSubscriptionInterceptor interceptor =
            new DriverSubscriptionInterceptor(decoder, identityClient,
                    riderIdentityClient, rideAccessClient, chatSessionAccessClient);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void driverTopicRequiresAuthenticatedOwnership() {
        Map<String, Object> session = new HashMap<>();
        Message<?> unauthenticated = subscription(session, "/topic/drivers/55");
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(unauthenticated, channel));

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256")
                .subject("driver@example.com")
                .claim("userId", 42L)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).build();
        when(decoder.decode("token")).thenReturn(jwt);
        interceptor.preSend(connect(session), channel);

        when(identityClient.belongsTo(42L, 55L)).thenReturn(true);
        Message<?> owned = subscription(session, "/topic/drivers/55");
        assertSame(owned, interceptor.preSend(owned, channel));
        verify(identityClient).belongsTo(42L, 55L);

        Message<?> otherDriver = subscription(session, "/topic/drivers/56");
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(otherDriver, channel));
    }

    @Test
    void chatTopicRequiresAuthenticatedParticipant() {
        Map<String, Object> session = new HashMap<>();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256")
                .subject("rider@example.com")
                .claim("userId", 42L)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).build();
        when(decoder.decode("token")).thenReturn(jwt);
        interceptor.preSend(connect(session), channel);

        when(chatSessionAccessClient.belongsTo(42L, 77L)).thenReturn(true);
        Message<?> owned = subscription(session, "/topic/chat/session/77");
        assertSame(owned, interceptor.preSend(owned, channel));
        verify(chatSessionAccessClient).belongsTo(42L, 77L);

        when(chatSessionAccessClient.belongsTo(42L, 78L)).thenReturn(false);
        Message<?> otherChat = subscription(session, "/topic/chat/session/78");
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(otherChat, channel));
    }

    @Test
    void unauthenticatedChatWritesAndDirectTopicPublishingAreDenied() {
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                send(new HashMap<>(), "/app/chat.send"), channel));
        Map<String, Object> authenticated = Map.of(
                DriverSubscriptionInterceptor.AUTH_USER_ID_ATTRIBUTE, 42L);
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                send(authenticated, "/topic/chat/session/77"), channel));
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                send(authenticated, "/app/chat.delete"), channel));
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                send(authenticated, "/app/chat.update"), channel));
    }

    @Test
    void authenticatedSendAndReplyArePassedToTheDurableController() {
        Map<String, Object> authenticated = Map.of(
                DriverSubscriptionInterceptor.AUTH_USER_ID_ATTRIBUTE, 42L);
        Message<?> send = send(authenticated, "/app/chat.send");
        Message<?> reply = send(authenticated, "/app/chat.reply");
        assertSame(send, interceptor.preSend(send, channel));
        assertSame(reply, interceptor.preSend(reply, channel));
    }

    @Test
    void riderAndRideTopicsRequireVerifiedParticipantOwnership() {
        Map<String, Object> session = new HashMap<>();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256")
                .subject("rider@example.com").claim("userId", 42L)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        when(decoder.decode("token")).thenReturn(jwt);
        interceptor.preSend(connect(session), channel);

        when(riderIdentityClient.belongsTo(42L, 77L)).thenReturn(true);
        Message<?> ownedRider = subscription(session, "/topic/riders/77");
        assertSame(ownedRider, interceptor.preSend(ownedRider, channel));
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                subscription(session, "/topic/riders/78"), channel));

        when(rideAccessClient.belongsTo(42L, 99L)).thenReturn(true);
        Message<?> ownedRide = subscription(session, "/topic/rides/99");
        assertSame(ownedRide, interceptor.preSend(ownedRide, channel));
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                subscription(session, "/topic/rides/100"), channel));
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                subscription(session, "/topic/rides"), channel));
    }

    @Test
    void directRideTopicPublishingAndUnauthenticatedLocationWritesAreDenied() {
        Map<String, Object> authenticated = Map.of(
                DriverSubscriptionInterceptor.AUTH_USER_ID_ATTRIBUTE, 42L);
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                send(authenticated, "/topic/riders/77"), channel));
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                send(new HashMap<>(), "/app/location"), channel));
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                connectWithoutAuthorization(new HashMap<>()), channel));
    }

    private Message<?> connect(Map<String, Object> session) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(session);
        accessor.addNativeHeader("Authorization", "Bearer token");
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> connectWithoutAuthorization(Map<String, Object> session) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(session);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> subscription(Map<String, Object> session, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionAttributes(session);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> send(Map<String, Object> session, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionAttributes(session);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
