package com.swiftapp_backend.userservice.user.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ApplyOfferRequestEvent {
    private Long offerId;
    private Long authUserId;
    private Long riderId;
}
