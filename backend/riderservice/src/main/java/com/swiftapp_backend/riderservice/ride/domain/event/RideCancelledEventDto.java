package com.swiftapp_backend.riderservice.ride.domain.event;

import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RideCancelledEventDto extends BaseRideEventDto {

    private Long cancelledBy;

    private ChangedByType cancelledByType;

    private String reason;

    private LocalDateTime cancelledAt;
}