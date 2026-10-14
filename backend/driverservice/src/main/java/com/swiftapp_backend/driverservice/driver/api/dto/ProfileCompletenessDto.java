package com.swiftapp_backend.driverservice.driver.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of judging whether a driver's profile is complete.
 *
 * <p>Returned by {@code GET /api/v1/driver/profile/completeness}, which the
 * driver app calls when it opens. {@code complete} is true only when every
 * section (personal details, documents, vehicle) is complete.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCompletenessDto {

    private boolean complete;

    private SectionStatus personal;

    private SectionStatus documents;

    private SectionStatus vehicle;

    /** Flat list of every missing item across all sections. */
    private List<String> missing;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionStatus {
        private boolean complete;
        private List<String> missing;
    }
}
