package com.swiftapp_backend.matchingservice.matching.domain.events;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverStatusUpdateEvent {
    private Long driverId;
    private DriverStatusEventEnum driverStatus;
}
