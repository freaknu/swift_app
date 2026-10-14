package com.swiftapp_backend.authservice.authentication.api.controller;

import com.swiftapp_backend.authservice.authentication.api.dto.*;
import com.swiftapp_backend.authservice.authentication.application.service.AuthService;
import com.swiftapp_backend.authservice.shared.ApiResponseFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Authentication APIs",
        description = """
                APIs responsible for user authentication, OTP verification,
                account registration, login, JWT generation,
                and device/session management.
                """
)
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Send OTP",
            description = """
                    Sends a One-Time Password (OTP) to the user's registered email.

                    The OTP is stored temporarily in Redis and expires automatically.
                    This endpoint is typically used before account registration
                    or email verification.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/sendOtp/{email}")
    public ResponseEntity<ApiResponseFormat<String>> sendOtp(
            @Parameter(
                    description = "Registered email address",
                    example = "john@example.com"
            )
            @PathVariable String email) {

        var res = authService.sendOtp(email);

        return ResponseEntity.ok(
                ApiResponseFormat.<String>builder()
                        .data(res)
                        .statusCode(HttpStatus.OK.value())
                        .errors(null)
                        .message("OTP Sent Successfully")
                        .endpoint("/api/auth/sendOtp/" + email)
                        .success(true)
                        .build()
        );
    }

    @Operation(
            summary = "Verify OTP",
            description = """
                    Verifies the OTP entered by the user.

                    Returns true if the OTP is valid and not expired. The code remains in Redis
                    until its original expiry or an authenticated home-ready acknowledgement.
                    Returns an error if the OTP has expired or is invalid.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP"),
            @ApiResponse(responseCode = "404", description = "OTP not found or expired")
    })
    @GetMapping("/verifyOtp/{email}/{otp}")
    public ResponseEntity<ApiResponseFormat<Boolean>> verifyOtp(

            @Parameter(
                    description = "User email",
                    example = "john@example.com"
            )
            @PathVariable String email,

            @Parameter(
                    description = "6-digit OTP",
                    example = "123456"
            )
            @PathVariable int otp) throws Exception {

        var res = authService.isCorrectOtp(email, otp);

        HttpStatus status = res ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
                ApiResponseFormat.<Boolean>builder()
                        .data(res)
                        .statusCode(status.value())
                        .errors(null)
                        .message(res ? "OTP Verified Successfully" : "Invalid or expired OTP")
                        .endpoint("/api/v1/auth/verifyOtp")
                        .success(res)
                        .build()
        );
    }

    @Operation(summary = "Acknowledge home screen readiness", description = """
            Called after the authenticated app has displayed its home screen. Clears only the
            OTP claim issued to this access session. Safe to retry after expiry or a resend.
            """)
    @PostMapping("/home-ready")
    public ResponseEntity<ApiResponseFormat<Boolean>> homeReady(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody HomeReadyRequestDto data) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BadCredentialsException("A valid access token is required");
        }
        authService.acknowledgeHomeReady(authorization.substring(7), data.getOtpCompletionToken());
        return ResponseEntity.ok(ApiResponseFormat.<Boolean>builder()
                .data(true)
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Home screen acknowledged")
                .endpoint("/api/v1/auth/home-ready")
                .build());
    }

    @Operation(
            summary = "User Login",
            description = """
                    Authenticates an existing user.

                    On successful authentication the API returns:

                    • JWT Access Token
                    • Refresh Token
                    • User information
                    • Device information

                    Device metadata such as operating system,
                    IP address, and device type are also stored.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponseFormat<UserAuthResponseDto>> login(

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login request payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequestDto.class)
                    )
            )
            @RequestBody LoginRequestDto data,

            HttpServletRequest req

    ) throws Exception {


        var res = authService.login(data, req);

        return ResponseEntity.ok(
                ApiResponseFormat.<UserAuthResponseDto>builder()
                        .data(res)
                        .endpoint("/api/auth/login")
                        .message("Logged in Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .errors(null)
                        .success(true)
                        .build()
        );
    }

    @Operation(
            summary = "Create Account",
            description = """
                    Registers a new user account.

                    After successful registration the system:

                    • Creates the user
                    • Generates JWT tokens
                    • Stores login device information
                    • Creates an active session
                    • Returns authentication details
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "User already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponseFormat<UserAuthResponseDto>> signUp(

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Registration request payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateAccountRequestDto.class)
                    )
            )
            @RequestBody CreateAccountRequestDto data,

            HttpServletRequest req

    ) {


        var res = authService.createAccount(data, req);


        return ResponseEntity.ok(
                ApiResponseFormat.<UserAuthResponseDto>builder()
                        .data(res)
                        .endpoint("/api/v1/auth/signup")
                        .message("Created Account Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .errors(null)
                        .success(true)
                        .build()
        );
    }

    @Operation(summary = "Google OAuth Login", description = "Authenticates user using Google OAuth token")
    @PostMapping("/google")
    public ResponseEntity<ApiResponseFormat<UserAuthResponseDto>> googleLogin(
            @RequestBody SocialLoginRequestDto data,
            HttpServletRequest req) {
        data.setProvider("GOOGLE");
        var res = authService.socialLogin(data, req);
        return ResponseEntity.ok(
                ApiResponseFormat.<UserAuthResponseDto>builder()
                        .data(res)
                        .endpoint("/api/v1/auth/google")
                        .message("Google Login Successful")
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .build()
        );
    }

    @Operation(summary = "Apple Login", description = "Authenticates user using Apple Sign-In")
    @PostMapping("/apple")
    public ResponseEntity<ApiResponseFormat<UserAuthResponseDto>> appleLogin(
            @RequestBody SocialLoginRequestDto data,
            HttpServletRequest req) {
        data.setProvider("APPLE");
        var res = authService.socialLogin(data, req);
        return ResponseEntity.ok(
                ApiResponseFormat.<UserAuthResponseDto>builder()
                        .data(res)
                        .endpoint("/api/v1/auth/apple")
                        .message("Apple Login Successful")
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .build()
        );
    }

    @Operation(summary = "Refresh Token", description = "Generates new access & refresh tokens using valid refresh token")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponseFormat<UserAuthResponseDto>> refreshToken(
            @RequestBody RefreshTokenRequestDto data,
            HttpServletRequest req) {
        var res = authService.refreshToken(data, req);
        return ResponseEntity.ok(
                ApiResponseFormat.<UserAuthResponseDto>builder()
                        .data(res)
                        .endpoint("/api/v1/auth/refresh-token")
                        .message("Token Refreshed Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .build()
        );
    }

    @Operation(summary = "Logout", description = "Invalidates user session and tokens")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseFormat<Boolean>> logout(@RequestBody LogoutRequestDto data) {
        authService.logout(data);
        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .data(true)
                        .endpoint("/api/v1/auth/logout")
                        .message("Logged Out Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .build()
        );
    }

    @Operation(summary = "Validate Token", description = "Validates JWT access token")
    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponseFormat<TokenValidationResponseDto>> validateToken(
            @RequestBody TokenValidationRequestDto data) {
        var res = authService.validateToken(data);
        return ResponseEntity.ok(
                ApiResponseFormat.<TokenValidationResponseDto>builder()
                        .data(res)
                        .endpoint("/api/v1/auth/validate-token")
                        .message("Token Validation Checked")
                        .statusCode(HttpStatus.OK.value())
                        .success(res.isValid())
                        .build()
        );
    }

    @Operation(summary = "Request Password Reset", description = "Sends OTP for password reset")
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponseFormat<String>> requestPasswordReset(
            @RequestBody PasswordResetRequestDto data) {
        var res = authService.sendOtp(data.getEmail());
        return ResponseEntity.ok(
                ApiResponseFormat.<String>builder()
                        .data(res)
                        .endpoint("/api/v1/auth/password-reset/request")
                        .message("Password Reset OTP Sent Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .build()
        );
    }

    @Operation(summary = "Confirm Password Reset", description = "Verifies OTP and resets password")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponseFormat<Boolean>> confirmPasswordReset(
            @RequestBody PasswordResetConfirmDto data) throws Exception {
        authService.confirmPasswordReset(data);
        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .data(true)
                        .endpoint("/api/v1/auth/password-reset/confirm")
                        .message("Password Reset Confirmed Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .build()
        );
    }

    @Operation(summary = "Verify Email", description = "Verifies user email via OTP")
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponseFormat<Boolean>> verifyEmail(
            @RequestBody EmailVerificationRequestDto data) throws Exception {
        boolean valid = authService.verifyEmail(data.getEmail(), data.getOtp());
        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .data(valid)
                        .endpoint("/api/v1/auth/verify-email")
                        .message(valid ? "Email Verified Successfully" : "Invalid OTP")
                        .statusCode(valid ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value())
                        .success(valid)
                        .build()
        );
    }
}
