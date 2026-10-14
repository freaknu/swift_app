package com.swiftapp_backend.paymentservice.payment.application.port;
import com.swiftapp_backend.paymentservice.payment.domain.model.CompletedRide;
public interface RideLookupPort {
    CompletedRide getParticipantRide(Long rideId, Long authUserId);
    Long riderIdForAuthUser(Long authUserId);
}
