package com.swiftapp_backend.driverservice.driver.application.mapper;

import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VehicleMapperVerificationTest {
    @Test
    void responsePreservesVerifiedAndUnknownReviewState() {
        assertEquals(Boolean.TRUE, VehicleMapper.toVehicleResponse(
                VehicleModel.builder().isVerified(true).build()).getIsVerified());
        assertNull(VehicleMapper.toVehicleResponse(
                VehicleModel.builder().isVerified(null).build()).getIsVerified());
    }
}
