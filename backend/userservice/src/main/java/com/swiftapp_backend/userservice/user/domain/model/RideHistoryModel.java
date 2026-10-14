package com.swiftapp_backend.userservice.user.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideHistoryModel {

    private Long id;

    private Long authUserId;

    private Long riderProfileId;

    private Long rideId;

    private double pickUpLatitude;
    private double pickUpLongitude;

    private double destinationLatitude;
    private double destinationLongitude;

    private LocalDateTime createdAt;
}
