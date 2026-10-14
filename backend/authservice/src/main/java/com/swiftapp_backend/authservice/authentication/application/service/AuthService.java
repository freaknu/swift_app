package com.swiftapp_backend.authservice.authentication.application.service;

import com.swiftapp_backend.authservice.authentication.api.dto.*;
import com.swiftapp_backend.authservice.authentication.application.port.EventPublisher;
import com.swiftapp_backend.authservice.authentication.application.port.OtpStore;
import com.swiftapp_backend.authservice.authentication.application.port.TokenProvider;
import com.swiftapp_backend.authservice.authentication.domain.enums.AppUserRoles;
import com.swiftapp_backend.authservice.authentication.domain.event.OtpGeneratedEvent;
import com.swiftapp_backend.authservice.authentication.domain.event.OtpSendNotificationDto;
import com.swiftapp_backend.authservice.authentication.domain.event.UserCreateEvent;
import com.swiftapp_backend.authservice.authentication.domain.event.UserDeletedEvent;
import com.swiftapp_backend.authservice.authentication.domain.event.UserLoggedInEvent;
import com.swiftapp_backend.authservice.authentication.domain.model.AppUser;
import com.swiftapp_backend.authservice.authentication.domain.model.UserCredentials;
import com.swiftapp_backend.authservice.authentication.domain.repository.AppUserRepository;
import com.swiftapp_backend.authservice.authentication.domain.repository.UserCredentialRepository;
import com.swiftapp_backend.authservice.shared.UserAlreadyFoundException;
import com.swiftapp_backend.authservice.shared.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ua_parser.Client;
import ua_parser.Parser;

import java.time.Duration;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final OtpStore otpStore;
    private final TokenProvider tokenProvider;
    private final AppUserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final EventPublisher eventPublisher;

    public String sendOtp(String email) {

        int otp = ThreadLocalRandom.current().nextInt(100000, 1000000);

        log.info("Generated OTP for {}", email);

        String redisKey = "userOtp:" + email;

        String subject = "Your OTP Verification Code";

        String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>OTP Verification</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7fa;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: #f4f7fa; padding: 40px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" border="0" style="background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); overflow: hidden;">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600; letter-spacing: -0.5px;">
                                            🚗 SwiftApp
                                        </h1>
                                        <p style="margin: 10px 0 0 0; color: #e8e8ff; font-size: 14px;">
                                            Secure Verification Code
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 50px 40px;">
                                        <h2 style="margin: 0 0 20px 0; color: #2d3748; font-size: 24px; font-weight: 600;">
                                            Verify Your Account
                                        </h2>
                                        <p style="margin: 0 0 30px 0; color: #4a5568; font-size: 16px; line-height: 1.6;">
                                            Hello! You've requested a verification code to access your account. 
                                            Please use the following OTP to complete your authentication:
                                        </p>
                                        
                                        <!-- OTP Box -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                            <tr>
                                                <td align="center" style="padding: 30px 0;">
                                                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 10px; padding: 25px 50px; display: inline-block;">
                                                        <p style="margin: 0; color: #ffffff; font-size: 14px; font-weight: 500; letter-spacing: 2px; text-transform: uppercase;">
                                                            Your OTP Code
                                                        </p>
                                                        <h1 style="margin: 15px 0 0 0; color: #ffffff; font-size: 42px; font-weight: 700; letter-spacing: 8px; font-family: 'Courier New', monospace;">
                                                            %s
                                                        </h1>
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Warning Box -->
                                        <div style="background-color: #fff5f5; border-left: 4px solid #f56565; border-radius: 6px; padding: 20px; margin: 30px 0;">
                                            <p style="margin: 0; color: #c53030; font-size: 14px; line-height: 1.6;">
                                                ⚠️ <strong>Important:</strong> This OTP is valid for <strong>1 minute only</strong>. 
                                                Do not share this code with anyone, including SwiftApp staff.
                                            </p>
                                        </div>
                                        
                                        <p style="margin: 30px 0 0 0; color: #718096; font-size: 14px; line-height: 1.6;">
                                            If you didn't request this code, please ignore this email or contact our support team immediately.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f7fafc; padding: 30px 40px; border-top: 1px solid #e2e8f0;">
                                        <p style="margin: 0 0 10px 0; color: #718096; font-size: 13px; text-align: center; line-height: 1.6;">
                                            This is an automated message. Please do not reply to this email.
                                        </p>
                                        <p style="margin: 0; color: #a0aec0; font-size: 12px; text-align: center;">
                                            © 2026 SwiftApp. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(otp);

        OtpSendNotificationDto notification = OtpSendNotificationDto.builder()
                .email(email)
                .subject(subject)
                .htmlContent(htmlContent)
                .build();

        // Save OTP first
        otpStore.store(redisKey, otp, Duration.ofMinutes(1));

        // Publish event
        String TOPIC = "otp-notification";
        eventPublisher.publish(
                TOPIC,
                email,
                notification
        );

        // Also publish otp.generated event
        OtpGeneratedEvent otpEvent = OtpGeneratedEvent.builder()
                .email(email)
                .otp(otp)
                .generatedAt(LocalDateTime.now())
                .expiryMinutes(1)
                .build();
        eventPublisher.publish("otp.generated", email, otpEvent);

        return "OTP sent successfully";
    }

    public boolean isCorrectOtp(String email,int otp) throws Exception {
        return otpStore.verify("userOtp:" + email, otp);
    }

    @Transactional
    public boolean verifyEmail(String email, int otp) throws Exception {
        boolean valid = isCorrectOtp(email, otp);
        if (valid) {
            Optional<AppUser> user = userRepository.findByEmail(email);
            if (user.isPresent()) {
                AppUser appUser = user.get();
                appUser.setActive(true);
                userRepository.save(appUser);
            }
            return otpStore.consume("userOtp:" + email, otp);
        }
        return valid;
    }

    @Transactional
    public UserAuthResponseDto createAccount(CreateAccountRequestDto data, HttpServletRequest req)throws RuntimeException {
        String email = data.getEmail();
        Optional<AppUser> existingUser = userRepository.findByEmail(email);

        // if user is present in db ->
        if(existingUser.isPresent()) {
            throw new UserAlreadyFoundException("User Already Exists");
        }

        String otpCompletionToken = UUID.randomUUID().toString();
        // Signup has no OTP field: require the server-side verification performed immediately before it.
        // The access-token binding is installed below before returning any credentials.

        // if user is not present in db ->
        AppUser newUser = AppUser
                .builder()
                .email(data.getEmail())
                .isActive(true)
                .role(data.getRole())
                .name(data.getName())
                .accountLocked(false)
                .build();

        AppUser savedUser = userRepository.save(newUser);
        UserCreateEvent kafkaUserData = UserCreateEvent.builder()
                .authUserId(savedUser.getId())
                .email(savedUser.getEmail())
                .phoneNumber(data.getPhoneNumber())
                .name(savedUser.getName())
                .build();

        // generating the token
        String accessToken = tokenProvider.generateAccessToken(savedUser.getEmail(), savedUser.getId());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getEmail(), savedUser.getId());
        claimOtp(email, null, otpCompletionToken, accessToken);

        // accessing the device info
        String userAgent = req != null ? req.getHeader("User-Agent") : "";
        String ipAddress = req != null ? req.getHeader("X-Forwarded-For") : "127.0.0.1";

        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = req != null ? req.getRemoteAddr() : "127.0.0.1";
        }
        Parser parser = new Parser();

        Client client = parser.parse(userAgent != null ? userAgent : "");

        String os = client.os.family;
        String deviceModel = client.device.family;

        // other user details
        UserCredentials userCredentials = UserCredentials
                .builder()
                .userId(savedUser.getId())
                .deviceName(deviceModel)
                .ipAddress(ipAddress)
                .isAppLocked(false)
                .os(os)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isLoggedOut(false)
                .build();

        userCredentialRepository.save(userCredentials);

        if(savedUser.getRole().equals(AppUserRoles.RIDER)) {
            eventPublisher.publish("user-created-topic", "user" + savedUser.getId(), kafkaUserData);
        } else if(savedUser.getRole().equals(AppUserRoles.DRIVER)) {
            eventPublisher.publish("driver-created-topic", "driver" + savedUser.getId(), kafkaUserData);
        }

        return UserAuthResponseDto
                .builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .deviceLocked(false)
                .accountLocked(false)
                .isActive(true)
                .role(savedUser.getRole())
                .otpCompletionToken(otpCompletionToken)
                .build();

    }

    @Transactional
    public UserAuthResponseDto login(LoginRequestDto data,HttpServletRequest req)  {
        String email = data.getEmail();
        Optional<AppUser> existingUser = userRepository.findByEmail(email);

        // if user is not present
        if(existingUser.isEmpty()) {
            throw new UserNotFoundException("User Not found Please SignUp");
        }

        String otpCompletionToken = UUID.randomUUID().toString();

        // if user is present generating the token
        AppUser savedUser = existingUser.get();
        String accessToken = tokenProvider.generateAccessToken(savedUser.getEmail(), savedUser.getId());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getEmail(), savedUser.getId());
        claimOtp(email, data.getOtp(), otpCompletionToken, accessToken);

        // accessing the device info
        String userAgent = req != null ? req.getHeader("User-Agent") : "";
        String ipAddress = req != null ? req.getHeader("X-Forwarded-For") : "127.0.0.1";

        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = req != null ? req.getRemoteAddr() : "127.0.0.1";
        }
        Parser parser = new Parser();

        Client client = parser.parse(userAgent != null ? userAgent : "");

        String os = client.os.family;
        String deviceModel = client.device.family;

        // other user details
        UserCredentials userCredentials = UserCredentials
                .builder()
                .userId(savedUser.getId())
                .deviceName(deviceModel)
                .ipAddress(ipAddress)
                .isAppLocked(false)
                .os(os)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isLoggedOut(false)
                .build();

        userCredentialRepository.save(userCredentials);

        // Publish user.loggedin
        UserLoggedInEvent loggedInEvent = UserLoggedInEvent.builder()
                .authUserId(savedUser.getId())
                .email(savedUser.getEmail())
                .deviceName(deviceModel)
                .ipAddress(ipAddress)
                .loggedInAt(LocalDateTime.now())
                .build();
        if(savedUser.getRole().equals(AppUserRoles.RIDER)) {
            eventPublisher.publish("user.loggedin", "user" + savedUser.getId(), loggedInEvent);
        }
        else if(savedUser.getRole().equals(AppUserRoles.DRIVER)) {
            eventPublisher.publish("driver.loggedin","driver"+savedUser.getId(),loggedInEvent);
        }
        return UserAuthResponseDto
                .builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .deviceLocked(false)
                .accountLocked(false)
                .isActive(true)
                .role(savedUser.getRole())
                .otpCompletionToken(otpCompletionToken)
                .build();
    }

    @Transactional
    public UserAuthResponseDto socialLogin(SocialLoginRequestDto data, HttpServletRequest req) {
        String email = data.getEmail();
        Optional<AppUser> existingUser = userRepository.findByEmail(email);

        AppUser user;
        if (existingUser.isEmpty()) {
            AppUser newUser = AppUser.builder()
                    .email(email)
                    .name(data.getName() != null ? data.getName() : "Social User")
                    .role(data.getRole() != null ? data.getRole() : AppUserRoles.RIDER)
                    .isActive(true)
                    .accountLocked(false)
                    .build();
            user = userRepository.save(newUser);

            UserCreateEvent kafkaUserData = UserCreateEvent.builder()
                    .authUserId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .phoneNumber("")
                    .build();
            eventPublisher.publish("user-created-topic", "user" + user.getId(), kafkaUserData);
        } else {
            user = existingUser.get();
        }

        String accessToken = tokenProvider.generateAccessToken(user.getEmail(), user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail(), user.getId());

        String userAgent = req != null ? req.getHeader("User-Agent") : "";
        String ipAddress = req != null ? req.getHeader("X-Forwarded-For") : "127.0.0.1";
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = req != null ? req.getRemoteAddr() : "127.0.0.1";
        }
        Parser parser = new Parser();
        Client client = parser.parse(userAgent != null ? userAgent : "");

        UserCredentials credentials = UserCredentials.builder()
                .userId(user.getId())
                .deviceName(client.device.family)
                .ipAddress(ipAddress)
                .os(client.os.family)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isAppLocked(false)
                .isLoggedOut(false)
                .build();
        userCredentialRepository.save(credentials);

        UserLoggedInEvent loggedInEvent = UserLoggedInEvent.builder()
                .authUserId(user.getId())
                .email(user.getEmail())
                .deviceName(client.device.family)
                .ipAddress(ipAddress)
                .loggedInAt(LocalDateTime.now())
                .build();
        eventPublisher.publish("user.loggedin", "user" + user.getId(), loggedInEvent);

        return UserAuthResponseDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .deviceLocked(false)
                .accountLocked(false)
                .isActive(true)
                .role(user.getRole())
                .build();
    }

    @Transactional
    public UserAuthResponseDto refreshToken(RefreshTokenRequestDto data, HttpServletRequest req) {
        String token = data.getRefreshToken();
        if (!tokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String email = tokenProvider.extractUsername(token);
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String newAccessToken = tokenProvider.generateAccessToken(user.getEmail(), user.getId());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getEmail(), user.getId());

        Optional<UserCredentials> credOpt = userCredentialRepository.findByRefreshToken(token);
        if (credOpt.isPresent()) {
            UserCredentials cred = credOpt.get();
            cred.setAccessToken(newAccessToken);
            cred.setRefreshToken(newRefreshToken);
            userCredentialRepository.save(cred);
        }

        return UserAuthResponseDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .deviceLocked(false)
                .accountLocked(false)
                .isActive(true)
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void logout(LogoutRequestDto data) {
        if (data.getRefreshToken() != null) {
            Optional<UserCredentials> cred = userCredentialRepository.findByRefreshToken(data.getRefreshToken());
            if (cred.isPresent()) {
                UserCredentials c = cred.get();
                c.setLoggedOut(true);
                userCredentialRepository.save(c);
            }
        } else if (data.getAccessToken() != null) {
            Optional<UserCredentials> cred = userCredentialRepository.findByAccessToken(data.getAccessToken());
            if (cred.isPresent()) {
                UserCredentials c = cred.get();
                c.setLoggedOut(true);
                userCredentialRepository.save(c);
            }
        }
    }

    public TokenValidationResponseDto validateToken(TokenValidationRequestDto data) {
        boolean valid = tokenProvider.validateToken(data.getToken());
        if (!valid) {
            return TokenValidationResponseDto.builder()
                    .valid(false)
                    .build();
        }
        String email = tokenProvider.extractUsername(data.getToken());
        Optional<AppUser> user = userRepository.findByEmail(email);
        return TokenValidationResponseDto.builder()
                .valid(true)
                .email(email)
                .userId(user.map(AppUser::getId).orElse(null))
                .build();
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmDto data) throws Exception {
        boolean valid = otpStore.consume("userOtp:" + data.getEmail(), data.getOtp());
        if (!valid) {
            throw new IllegalArgumentException("Invalid OTP for password reset");
        }
        // In this OTP-based auth model, verifying OTP confirms password/access
        log.info("Password reset verified successfully for {}", data.getEmail());
    }

    private void claimOtp(String email, Integer otp, String completionToken, String accessToken) {
        String key = "userOtp:" + email;
        String hash = sessionHash(accessToken);
        if (!otpStore.claimVerified(key, otp, completionToken, hash)) {
            throw new BadCredentialsException("OTP is invalid, expired, or already used. Request a new OTP.");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        otpStore.release(key, completionToken, hash);
                    }
                }
            });
        }
    }

    /** Called only after the app has mounted its role home screen. It is safe to retry. */
    public void acknowledgeHomeReady(String accessToken, String completionToken) {
        if (accessToken == null || accessToken.isBlank() || !tokenProvider.validateToken(accessToken)) {
            throw new BadCredentialsException("A valid access token is required");
        }
        String email = tokenProvider.extractUsername(accessToken);
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid user session"));
        UserCredentials session = userCredentialRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid access session"));
        if (session.isLoggedOut() || !user.getId().equals(session.getUserId())
                || user.isAccountLocked() || !user.isActive()) {
            throw new BadCredentialsException("Invalid access session");
        }
        if (completionToken == null || completionToken.isBlank() || completionToken.length() > 128) {
            throw new IllegalArgumentException("OTP completion token is required");
        }
        otpStore.complete("userOtp:" + email, completionToken, sessionHash(accessToken));
    }

    private static String sessionHash(String accessToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(accessToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Transactional
    public void handleUserDeleted(UserDeletedEvent event) {
        log.info("Handling user deletion for authUserId={}, email={}", event.getAuthUserId(), event.getEmail());
        if (event.getAuthUserId() != null) {
            userCredentialRepository.deleteAllByUserId(event.getAuthUserId());
            userRepository.findById(event.getAuthUserId()).ifPresent(user -> {
                user.setActive(false);
                userRepository.save(user);
            });
        } else if (event.getEmail() != null) {
            userRepository.findByEmail(event.getEmail()).ifPresent(user -> {
                userCredentialRepository.deleteAllByUserId(user.getId());
                user.setActive(false);
                userRepository.save(user);
            });
        }
    }
}
