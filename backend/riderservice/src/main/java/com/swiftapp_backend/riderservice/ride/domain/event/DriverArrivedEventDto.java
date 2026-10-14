package com.swiftapp_backend.riderservice.ride.domain.event;

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
public class DriverArrivedEventDto extends BaseRideEventDto {

    private LocalDateTime arrivedAt;
}