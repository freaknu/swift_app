package com.swiftapp_backend.driverservice.driver.application.mapper;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverEarningSummaryDto;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverEarningSummaryModel;
import org.springframework.stereotype.Component;

@Component
public class DriverEarningMapper {

    public DriverEarningSummaryDto toDto(DriverEarningSummaryModel model) {
        if (model == null) return null;
        return DriverEarningSummaryDto.builder()
                .id(model.getId())
                .thisMonthIncome(model.getThisMonthIncome())
                .thisWeekIncome(model.getThisWeekIncome())
                .thisLifetimeIncome(model.getThisLifetimeIncome())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
