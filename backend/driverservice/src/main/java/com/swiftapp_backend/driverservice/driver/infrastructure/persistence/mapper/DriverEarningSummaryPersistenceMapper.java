package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverEarningSummaryModel;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEarningSummaryEntity;
import org.springframework.stereotype.Component;

@Component
public class DriverEarningSummaryPersistenceMapper {

    public DriverEarningSummaryModel toDomain(DriverEarningSummaryEntity entity) {
        if (entity == null) return null;
        return DriverEarningSummaryModel.builder()
                .id(entity.getId())
                .thisMonthIncome(entity.getThisMonthIncome())
                .thisWeekIncome(entity.getThisWeekIncome())
                .thisLifetimeIncome(entity.getThisLifetimeIncome())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public DriverEarningSummaryEntity toEntity(DriverEarningSummaryModel model) {
        if (model == null) return null;
        DriverEarningSummaryEntity entity = new DriverEarningSummaryEntity();
        entity.setId(model.getId());
        entity.setThisMonthIncome(model.getThisMonthIncome());
        entity.setThisWeekIncome(model.getThisWeekIncome());
        entity.setThisLifetimeIncome(model.getThisLifetimeIncome());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        return entity;
    }
}
