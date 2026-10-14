package com.swiftapp_backend.userservice.user.infrastructure.persistence.repository;

import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataAddressRepository extends JpaRepository<AddressEntity, Long> {
    List<AddressEntity> findAllByUser_Id(Long userId);
}
