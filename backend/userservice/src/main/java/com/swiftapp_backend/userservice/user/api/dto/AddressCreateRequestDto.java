package com.swiftapp_backend.userservice.user.api.dto;

import com.swiftapp_backend.userservice.user.domain.enums.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class AddressCreateRequestDto{
    private AddressType addressType;
    private String label;
    private String fullAddress;
    private String landmark;
    private String city;
    private String state;
    private String country;
    private String pinCode;
    private Double latitude;
    private Double longitude;
    private Boolean defaultAddress = false;
    private Boolean isFavouritePlace = false;
}