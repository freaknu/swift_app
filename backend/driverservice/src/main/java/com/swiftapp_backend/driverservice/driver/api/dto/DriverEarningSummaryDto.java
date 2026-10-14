package com.swiftapp_backend.driverservice.driver.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverEarningSummaryDto {
    private Long id;
    private Long thisMonthIncome;
    private Long thisWeekIncome;
    private Long thisLifetimeIncome;
    private LocalDateTime updatedAt;
}
