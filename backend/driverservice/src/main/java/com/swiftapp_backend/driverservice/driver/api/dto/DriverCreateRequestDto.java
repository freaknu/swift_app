package com.swiftapp_backend.driverservice.driver.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.domain.enums.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverCreateRequestDto {

 @NotBlank(message = "Driver name is required")
 @Size(min = 3, max = 100, message = "Driver name must be between 3 and 100 characters")
 private String driverName;

 @NotBlank(message = "Email is required")
 @Email(message = "Invalid email format")
 @Size(max = 100)
 private String email;

 @NotBlank(message = "Phone number is required")
 @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must contain exactly 10 digits")
 private String phoneNumber;

 @NotNull(message = "Gender is required")
 private Gender gender;

 private String profileImage;

 private DriverStatus driverStatus;

 @JsonAlias({"acceptingRides", "isAcceptingRides"})
 private Boolean isAcceptingRides;

 private DriverAvailability driverAvailability;

 private List<DriverPreferences> driverPreferences;

 private List<VehicleCreateRequestDto> allVehicles;

 private LocalDate dateOfBirth;

 private List<Language> languages;

 private List<DriverDocumentCreateRequestDto> allDocuments;
}
