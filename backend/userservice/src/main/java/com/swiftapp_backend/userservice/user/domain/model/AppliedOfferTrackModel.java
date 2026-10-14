package com.swiftapp_backend.userservice.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class AppliedOfferTrackModel {
    private Long id;

    private Long offerId;
    private Long authUserId;
    private Long riderId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
