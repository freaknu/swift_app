package com.swiftapp_backend.userservice.user.domain.repository;

import com.swiftapp_backend.userservice.user.domain.model.AddressModel;

import java.util.List;
import java.util.Optional;

public interface AddressRepository {
    AddressModel save(AddressModel address);

    Optional<AddressModel> findById(Long id);

    List<AddressModel> findAllByUser_Id(Long userId);
}
