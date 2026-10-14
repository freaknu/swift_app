package com.swiftapp_backend.userservice.user.api.controller;

import com.swiftapp_backend.userservice.user.api.dto.*;
import com.swiftapp_backend.userservice.user.application.service.RideHistoryService;
import com.swiftapp_backend.userservice.user.application.service.UserService;
import com.swiftapp_backend.userservice.shared.ApiResponseFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import com.swiftapp_backend.userservice.shared.ApiException;

@Slf4j
@RestController
@Tag(name = "User Controller",description = "User Controller for User Service")
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RideHistoryService rideHistoryService;

    @GetMapping("/getAllUsers")
    public ResponseEntity<ApiResponseFormat<List<UserResponseDto>>> getAllUsers(@RequestHeader("x-user-id") Long authUserId) {
        throw ApiException.forbidden("Listing all users requires an administrator role");
    }

    @PostMapping("/updateUser/{deviceId}")
    @Operation(description = "Update User")
    public ResponseEntity<ApiResponseFormat<UserResponseDto>> createUser(@RequestHeader("x-user-id") Long authUserId,@PathVariable String deviceId, @RequestBody UserCreateRequestDto data) {
        var res = userService.updateUser(authUserId, data);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<UserResponseDto>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("User Updated Successfully")
                        .data(res)
                        .errors(null)
                        .endpoint("/api/user/updateUser")
                        .build());
    }

    @GetMapping("/getUserDetails")
    public ResponseEntity<ApiResponseFormat<UserResponseDto>> getUserDetails(@RequestHeader("x-user-id") Long authUserId) throws Exception {
        var res = userService.getUserDetail(authUserId);
        log.info("under ========================= {}",authUserId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<UserResponseDto>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("User fetched Successfully")
                        .data(res)
                        .errors(null)
                        .endpoint("/api/user/getUserDetails/"+authUserId)
                        .build());
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<ApiResponseFormat<UserResponseDto>> deleteUser(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long userId) throws Exception {
        requireOwnPath(authUserId, userId);
        var res = userService.deactivateUser(userId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<UserResponseDto>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("User deleted Successfully")
                        .data(res)
                        .errors(null)
                        .endpoint("/api/user/delete/"+userId)
                        .build());
    }

    @PostMapping("/addAddress")
    public ResponseEntity<ApiResponseFormat<List<AddressCreateResponseDto>>> addAddress(@RequestHeader("x-user-id") Long authUserId, @RequestBody AddressCreateRequestDto data) throws Exception {
        var res = userService.addAddress(authUserId,data);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<AddressCreateResponseDto>>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Address added Successfully")
                        .data(res)
                        .errors(null)
                        .endpoint("/api/user/addAddress/"+authUserId)
                        .build());
    }


    @DeleteMapping("/deleteAddress/{userId}/{addressId}")
    public ResponseEntity<ApiResponseFormat<Boolean>> deleteAddress(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long userId,
                                                                   @PathVariable Long addressId) throws Exception {
        requireOwnPath(authUserId, userId);
        userService.deleteAddress(userId,addressId);

        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<Boolean>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Address deleted Successfully")
                        .data(true)
                        .errors(null)
                        .endpoint("/api/user/deleteAddress/"+userId+"/"+addressId)
                        .build());
    }

    @GetMapping("/getEmergencyContact/{userId}")
    public ResponseEntity<ApiResponseFormat<List<EmergencyContactDto>>> getEmergencyContact(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long userId) throws Exception {
        requireOwnPath(authUserId, userId);
        var res = userService.getAllContacts(userId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<EmergencyContactDto>>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Emergency Contact Fetched Successfully")
                        .data(res)
                        .errors(null)
                        .endpoint("/api/user/getEmergencyContact/"+userId)
                        .build());
    }

    @DeleteMapping("/deleteContact/{userId}/{contactId}")
    public ResponseEntity<ApiResponseFormat<List<EmergencyContactDto>>> deleteContact(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long userId,
                                                                                      @PathVariable Long contactId) throws Exception {
        requireOwnPath(authUserId, userId);
        var res = userService.deleteContact(userId,contactId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<EmergencyContactDto>>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Emergency Contact Deleted Successfully")
                        .data(res)
                        .errors(null)
                        .endpoint("/api/user/deleteContact/"+userId+"/"+contactId)
                        .build());
    }

    @PostMapping("/addContact/{userId}")
    public ResponseEntity<ApiResponseFormat<List<EmergencyContactDto>>> saveContact(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long userId,
                                                                                    @RequestBody List<EmergencyContactDto> datas) throws Exception {
        requireOwnPath(authUserId, userId);
        var res = userService.saveEmergencyContact(datas,userId);

        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<EmergencyContactDto>>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Emergency Contact Deleted Successfully")
                        .data(res)
                        .errors(null)
                        .endpoint("/api/user/addContact/"+userId)
                        .build());
    }


    @GetMapping("/getRideHistory")
    public ResponseEntity<ApiResponseFormat<List<RideHistoryResponseDto>>> getRideHistory(@RequestHeader("x-user-id") Long authUserId,
                                                                                          @RequestParam(defaultValue = "0") int page,
                                                                                          @RequestParam(defaultValue = "5") int pageSize) throws Exception {
        var res = rideHistoryService.getRideHistory(authUserId, page, pageSize);

        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<RideHistoryResponseDto>>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Ride History Fetched Successfully")
                        .data(res)
                        .errors(null)
                        .endpoint("/api/v1/getRideHistory"+authUserId)
                        .build());
    }

    @PostMapping("/profile-photo")
    @Operation(summary = "Update Profile Photo")
    public ResponseEntity<ApiResponseFormat<UserResponseDto>> updateProfilePhoto(
            @RequestHeader("x-user-id") Long authUserId,
            @RequestBody ProfilePhotoUpdateDto dto) throws Exception {
        var res = userService.updateProfilePhoto(authUserId, dto);
        return ResponseEntity.ok(ApiResponseFormat.<UserResponseDto>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Profile photo updated successfully")
                .data(res)
                .endpoint("/api/v1/user/profile-photo")
                .build());
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get User Preferences")
    public ResponseEntity<ApiResponseFormat<UserPreferencesDto>> getPreferences(
            @RequestHeader("x-user-id") Long authUserId) {
        var res = userService.getPreferences(authUserId);
        return ResponseEntity.ok(ApiResponseFormat.<UserPreferencesDto>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("User preferences fetched successfully")
                .data(res)
                .endpoint("/api/v1/user/preferences")
                .build());
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update User Preferences")
    public ResponseEntity<ApiResponseFormat<UserPreferencesDto>> updatePreferences(
            @RequestHeader("x-user-id") Long authUserId,
            @RequestBody UserPreferencesDto dto) {
        var res = userService.updatePreferences(authUserId, dto);
        return ResponseEntity.ok(ApiResponseFormat.<UserPreferencesDto>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("User preferences updated successfully")
                .data(res)
                .endpoint("/api/v1/user/preferences")
                .build());
    }

    @GetMapping("/favourite-places")
    @Operation(summary = "Get Favourite Places")
    public ResponseEntity<ApiResponseFormat<List<AddressCreateResponseDto>>> getFavouritePlaces(
            @RequestHeader("x-user-id") Long authUserId) throws Exception {
        var res = userService.getFavouritePlaces(authUserId);
        return ResponseEntity.ok(ApiResponseFormat.<List<AddressCreateResponseDto>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Favourite places fetched successfully")
                .data(res)
                .endpoint("/api/v1/user/favourite-places")
                .build());
    }

    @PostMapping("/favourite-places")
    @Operation(summary = "Add Favourite Place")
    public ResponseEntity<ApiResponseFormat<List<AddressCreateResponseDto>>> addFavouritePlace(
            @RequestHeader("x-user-id") Long authUserId,
            @RequestBody AddressCreateRequestDto dto) throws Exception {
        var res = userService.addFavouritePlace(authUserId, dto);
        return ResponseEntity.ok(ApiResponseFormat.<List<AddressCreateResponseDto>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Favourite place added successfully")
                .data(res)
                .endpoint("/api/v1/user/favourite-places")
                .build());
    }
    private void requireOwnPath(Long authUserId, Long userId) {
        if (!authUserId.equals(userId)) throw ApiException.forbidden("Account does not belong to the authenticated user");
    }
}
