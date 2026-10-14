package com.swiftapp_backend.analyticalservice.analytics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderAnalyticsResponseDto {
    private Long riderId;
    private Integer totalRides;
    private Integer completedRides;
    private Integer cancelledRides;
    private Double totalSpent;
    private Double averageRating;
    private Double averageFare;
    private String favoriteRoute;
    private Integer ridesThisMonth;
}
