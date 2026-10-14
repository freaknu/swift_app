package com.swiftapp_backend.driverservice.driver.infrastructure.messaging.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account-creation event published by the auth service on
 * {@code driver-created-topic} when a driver account is created.
 *
 * <p>Mirrors the auth service's {@code UserCreateEvent} contract:
 * {@code authUserId}, {@code email}, {@code name}, {@code phoneNumber}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverCreatedEvent {
    private Long authUserId;
    private String email;
    private String name;
    private String phoneNumber;
}
