package com.bikestore.api.service;

import com.bikestore.api.dto.request.LoginRequest;
import com.bikestore.api.dto.response.AccountStatusResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.Role;
import com.bikestore.api.event.SendEmailEvent;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.repository.UserRepository;
import com.bikestore.api.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private com.bikestore.api.mapper.UserMapper userMapper;
    @Mock
    private VerificationTokenService tokenService;
    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login should return EMAIL_NOT_VERIFIED error code when user is pending verification")
    void login_pendingVerification_throwsConflictWithCode() {
        LoginRequest request = new LoginRequest("pending@example.com", "Secret123");
        User user = User.builder()
                .email("pending@example.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));

        ConflictException ex = assertThrows(ConflictException.class, () -> authService.login(request));

        assertEquals("EMAIL_NOT_VERIFIED", ex.getErrorCode());
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("resend verification should send a new code for pending accounts")
    void resendVerification_pendingAccount_sendsCode() {
        User user = User.builder()
                .email("pending@example.com")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));
        when(tokenService.generateAndSaveVerificationToken(user)).thenReturn("123456");

        authService.resendVerificationCode("pending@example.com");

        verify(tokenService).generateAndSaveVerificationToken(user);
        verify(eventPublisher).publishEvent(any(SendEmailEvent.class));
    }

    @Test
    @DisplayName("resend verification should fail for already verified accounts")
    void resendVerification_verifiedAccount_throwsConflict() {
        User user = User.builder()
                .email("verified@example.com")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailVerified(true)
                .build();

        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user));

        ConflictException ex = assertThrows(ConflictException.class, () -> authService.resendVerificationCode("verified@example.com"));

        assertEquals("EMAIL_ALREADY_VERIFIED", ex.getErrorCode());
        verify(tokenService, never()).generateAndSaveVerificationToken(any());
    }

    @Test
    @DisplayName("account status should mark pendingVerification for active non-verified users")
    void getAccountStatus_pendingUser_marksPending() {
        User user = User.builder()
                .email("pending@example.com")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));

        AccountStatusResponse response = authService.getAccountStatus("pending@example.com");

        assertTrue(response.isActive());
        assertFalse(response.isEmailVerified());
        assertTrue(response.pendingVerification());
    }
}
