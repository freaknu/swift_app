package com.swiftapp_backend.userservice.user.application.mapper;

import com.swiftapp_backend.userservice.user.api.dto.EmergencyContactDto;
import com.swiftapp_backend.userservice.user.domain.model.EmergencyContactModel;
import org.springframework.stereotype.Component;

@Component
public class EmergencyContactMapper {
    public EmergencyContactModel toModel(EmergencyContactDto dto) {
        return EmergencyContactModel
                .builder()
                .contactType(dto.getContactType())
                .contactPersonName(dto.getContactPersonName())
                .contactPersonNumber(dto.getContactPersonNumber())
                .build();
    }

    public EmergencyContactDto toDto(EmergencyContactModel model) {
        return EmergencyContactDto
                .builder()
                .id(model.getId())
                .contactType(model.getContactType())
                .contactPersonName(model.getContactPersonName())
                .contactPersonNumber(model.getContactPersonNumber())
                .build();
    }
}
