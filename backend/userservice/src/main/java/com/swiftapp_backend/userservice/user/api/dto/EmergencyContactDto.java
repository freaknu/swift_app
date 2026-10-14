package com.swiftapp_backend.userservice.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class EmergencyContactDto {
    private Long id;
    private String contactType;
    private String contactPersonName;
    private String contactPersonNumber;
}
