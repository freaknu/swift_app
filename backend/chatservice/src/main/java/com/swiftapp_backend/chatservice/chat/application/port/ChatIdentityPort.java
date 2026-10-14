package com.swiftapp_backend.chatservice.chat.application.port;

/** Resolves profile ids belonging to the authenticated account. */
public interface ChatIdentityPort {
    default Identity identityForAuthUser(Long authUserId) {
        // Profile ids and auth account ids use different sequences. A
        // fallback must not authorize by numeric coincidence.
        return new Identity(authUserId, null, null);
    }

    record Identity(Long authUserId, Long riderId, Long driverId) {
    }
}
