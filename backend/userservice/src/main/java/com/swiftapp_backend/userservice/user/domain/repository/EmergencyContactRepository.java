package com.swiftapp_backend.userservice.user.domain.repository;

import com.swiftapp_backend.userservice.user.domain.model.EmergencyContactModel;

import java.util.List;
import java.util.Optional;

public interface EmergencyContactRepository {
    Optional<EmergencyContactModel> findById(Long id);

    List<EmergencyContactModel> saveAll(List<EmergencyContactModel> contacts);

    List<EmergencyContactModel> findAllByUser_Id(Long userId);
}
