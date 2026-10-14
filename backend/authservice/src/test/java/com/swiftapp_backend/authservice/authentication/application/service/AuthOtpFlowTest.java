package com.swiftapp_backend.authservice.authentication.application.service;

import com.swiftapp_backend.authservice.authentication.api.dto.*;
import com.swiftapp_backend.authservice.authentication.application.port.*;
import com.swiftapp_backend.authservice.authentication.domain.enums.AppUserRoles;
import com.swiftapp_backend.authservice.authentication.domain.model.*;
import com.swiftapp_backend.authservice.authentication.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthOtpFlowTest {
    private final OtpStore otp = mock(OtpStore.class);
    private final TokenProvider tokens = mock(TokenProvider.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final UserCredentialRepository sessions = mock(UserCredentialRepository.class);
    private final EventPublisher events = mock(EventPublisher.class);
    private final AuthService service = new AuthService(otp, tokens, users, sessions, events);
    private final AppUser user = AppUser.builder().id(7L).email("rider@example.test")
            .name("Rider").role(AppUserRoles.RIDER).isActive(true).build();

    @BeforeEach void account() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokens.generateAccessToken(user.getEmail(), 7L)).thenReturn("access");
        when(tokens.generateRefreshToken(user.getEmail(), 7L)).thenReturn("refresh");
    }

    @Test void verificationDoesNotDeleteOtpOnSuccessOrFailure() throws Exception {
        when(otp.verify("userOtp:" + user.getEmail(), 123456)).thenReturn(true);
        assertTrue(service.isCorrectOtp(user.getEmail(), 123456));
        assertFalse(service.isCorrectOtp(user.getEmail(), 999999));
        verify(otp, never()).delete(anyString());
        verify(otp, never()).consume(anyString(), anyInt());
    }

    @Test void invalidOrUnverifiedLoginCannotSaveSessionOrPublishLogin() {
        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequestDto(user.getEmail(), 999999), null));
        verify(otp).claimVerified(eq("userOtp:" + user.getEmail()), eq(999999), anyString(), anyString());
        verifyNoInteractions(sessions, events);
    }

    @Test void validLoginReturnsClaimTokenButDoesNotDeleteOtp() {
        when(otp.claimVerified(anyString(), eq(123456), anyString(), anyString())).thenReturn(true);
        UserAuthResponseDto result = service.login(new LoginRequestDto(user.getEmail(), 123456), null);
        assertNotNull(result.getOtpCompletionToken());
        assertFalse(result.getOtpCompletionToken().isBlank());
        assertEquals("access", result.getAccessToken());
        verify(sessions).save(any(UserCredentials.class));
        verify(otp, never()).delete(anyString());
        verify(otp, never()).complete(anyString(), anyString(), anyString());
    }

    @Test void failedTransactionReleasesOnlyItsClaimForRetry() {
        when(otp.claimVerified(anyString(), eq(123456), anyString(), anyString())).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            var response = service.login(new LoginRequestDto(user.getEmail(), 123456), null);
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(otp).release(eq("userOtp:" + user.getEmail()), eq(response.getOtpCompletionToken()), anyString());
            verify(otp, never()).delete(anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test void signupRequiresPriorVerificationBeforeItPublishesOrReturnsCredentials() {
        when(users.findByEmail("new@example.test")).thenReturn(Optional.empty());
        AppUser newUser = AppUser.builder().id(8L).email("new@example.test")
                .role(AppUserRoles.RIDER).isActive(true).build();
        when(users.save(any())).thenReturn(newUser);
        when(tokens.generateAccessToken("new@example.test", 8L)).thenReturn("new-access");
        assertThrows(BadCredentialsException.class, () -> service.createAccount(
                new CreateAccountRequestDto("new@example.test", "New", "", AppUserRoles.RIDER), null));
        verify(otp).claimVerified(eq("userOtp:new@example.test"), isNull(), anyString(), anyString());
        verifyNoInteractions(sessions, events);
    }

    @Test void homeAcknowledgementRequiresValidStoredAccessSessionAndDerivesItsAccount() {
        when(tokens.validateToken("access")).thenReturn(true);
        when(tokens.extractUsername("access")).thenReturn(user.getEmail());
        when(sessions.findByAccessToken("access")).thenReturn(Optional.of(UserCredentials.builder()
                .userId(7L).accessToken("access").build()));
        service.acknowledgeHomeReady("access", "completion");
        service.acknowledgeHomeReady("access", "completion");
        verify(otp, times(2)).complete(eq("userOtp:" + user.getEmail()), eq("completion"), anyString());
    }

    @Test void missingForgedExpiredRefreshAndOtherUserTokensCannotClearOtp() {
        for (String token : new String[]{null, "", "forged", "expired"}) {
            assertThrows(BadCredentialsException.class, () -> service.acknowledgeHomeReady(token, "completion"));
        }
        when(tokens.validateToken("refresh")).thenReturn(true);
        when(tokens.extractUsername("refresh")).thenReturn(user.getEmail());
        assertThrows(BadCredentialsException.class,
                () -> service.acknowledgeHomeReady("refresh", "completion"));
        when(tokens.validateToken("access")).thenReturn(true);
        when(tokens.extractUsername("access")).thenReturn(user.getEmail());
        when(sessions.findByAccessToken("access")).thenReturn(Optional.of(UserCredentials.builder()
                .userId(99L).accessToken("access").build()));
        assertThrows(BadCredentialsException.class,
                () -> service.acknowledgeHomeReady("access", "completion"));
        verifyNoInteractions(otp);
    }

    @Test void loggedOutOrLockedOrInactiveSessionCannotClearOtp() {
        when(tokens.validateToken("access")).thenReturn(true);
        when(tokens.extractUsername("access")).thenReturn(user.getEmail());
        UserCredentials session = UserCredentials.builder().userId(7L).isLoggedOut(true).build();
        when(sessions.findByAccessToken("access")).thenReturn(Optional.of(session));
        assertThrows(BadCredentialsException.class, () -> service.acknowledgeHomeReady("access", "completion"));
        session.setLoggedOut(false);
        user.setAccountLocked(true);
        assertThrows(BadCredentialsException.class, () -> service.acknowledgeHomeReady("access", "completion"));
        user.setAccountLocked(false);
        user.setActive(false);
        assertThrows(BadCredentialsException.class, () -> service.acknowledgeHomeReady("access", "completion"));
        verifyNoInteractions(otp);
    }

    @Test void passwordResetAndEmailVerificationStillConsumeOnlyOnSuccessfulNonLoginOperation() throws Exception {
        PasswordResetConfirmDto reset = new PasswordResetConfirmDto();
        reset.setEmail(user.getEmail()); reset.setOtp(123456);
        assertThrows(IllegalArgumentException.class, () -> service.confirmPasswordReset(reset));
        when(otp.consume("userOtp:" + user.getEmail(), 123456)).thenReturn(true);
        service.confirmPasswordReset(reset);
        when(otp.verify("userOtp:" + user.getEmail(), 123456)).thenReturn(true);
        assertTrue(service.verifyEmail(user.getEmail(), 123456));
        verify(users).save(user);
        verify(otp, times(3)).consume("userOtp:" + user.getEmail(), 123456);
    }
}
