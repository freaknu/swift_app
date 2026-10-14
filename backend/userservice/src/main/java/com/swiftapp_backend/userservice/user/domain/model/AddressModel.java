package com.swiftapp_backend.userservice.user.domain.model;

import com.swiftapp_backend.userservice.user.domain.enums.AddressType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressModel {

    private Long id;

    // Home, Work, etc.
    private AddressType addressType;

    // Home, Office, Mom's House...
    private String label;

    private String fullAddress;

    private String landmark;

    private String city;

    private String state;

    private String country;

    private String pinCode;

    private Double latitude;

    private Double longitude;


    private Boolean isFavouritePlace;

    private Boolean defaultAddress = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private Boolean active = true;

    private UserModel user;
}
