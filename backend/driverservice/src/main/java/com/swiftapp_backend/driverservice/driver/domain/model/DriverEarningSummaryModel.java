package com.swiftapp_backend.driverservice.driver.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverEarningSummaryModel {
    private Long id;
    private Long thisMonthIncome;
    private Long thisWeekIncome;
    private Long thisLifetimeIncome;
    private DriverModel driver;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
