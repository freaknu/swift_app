package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.entity;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long driverId;

    private double latitude;

    private double longitude;

    private DriverStatusEventEnum driverStatus;

    private VehicleTypeEnum vehicleType;
}
