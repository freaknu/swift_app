package com.swiftapp_backend.userservice.user.infrastructure.persistence.repository;

import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.EmergencyContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataEmergencyContactRepository extends JpaRepository<EmergencyContactEntity, Long> {
    List<EmergencyContactEntity> findAllByUser_Id(Long userId);
}
