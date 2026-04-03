package com.bikestore.api.controller;

import com.bikestore.api.dto.request.*;
import com.bikestore.api.dto.response.AuthResponse;
import com.bikestore.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("Should return 201 CREATED with AuthResponse when registration succeeds")
        void register_success_returns201() {
            RegisterRequest request = new RegisterRequest("Joe", "Luani", "joe@example.com", "Secret123");
            AuthResponse expected = new AuthResponse("", "User registered successfully. Please check your email for the verification code.");

            when(authService.register(request)).thenReturn(expected);

            ResponseEntity<AuthResponse> response = authController.register(request);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(expected.token(), response.getBody().token());
            assertEquals(expected.message(), response.getBody().message());
            verify(authService, times(1)).register(request);
        }

        @Test
        @DisplayName("Should delegate to AuthService exactly once")
        void register_delegatesToService() {
            RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane@example.com", "Pass1234");
            AuthResponse expected = new AuthResponse("tok", "msg");

            when(authService.register(request)).thenReturn(expected);

            authController.register(request);

            verify(authService).register(request);
            verifyNoMoreInteractions(authService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("Should return 200 OK with AuthResponse when login succeeds")
        void login_success_returns200() {
            LoginRequest request = new LoginRequest("joe@example.com", "Secret123");
            AuthResponse expected = new AuthResponse("jwt-token", "Login successful");

            when(authService.login(request)).thenReturn(expected);

            ResponseEntity<AuthResponse> response = authController.login(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("jwt-token", response.getBody().token());
            assertEquals("Login successful", response.getBody().message());
            verify(authService, times(1)).login(request);
        }

        @Test
        @DisplayName("Should propagate service exceptions")
        void login_serviceThrows_propagates() {
            LoginRequest request = new LoginRequest("bad@example.com", "wrong");

            when(authService.login(request)).thenThrow(new RuntimeException("Invalid credentials"));

            assertThrows(RuntimeException.class, () -> authController.login(request));
            verify(authService).login(request);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/google")
    class GoogleLogin {

        @Test
        @DisplayName("Should return 200 OK with AuthResponse when Google login succeeds")
        void loginWithGoogle_success_returns200() {
            GoogleLoginRequest request = new GoogleLoginRequest("google-id-token");
            AuthResponse expected = new AuthResponse("jwt-google-token", "Google login successful");

            when(authService.loginWithGoogle("google-id-token")).thenReturn(expected);

            ResponseEntity<AuthResponse> response = authController.loginWithGoogle(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("jwt-google-token", response.getBody().token());
            assertEquals("Google login successful", response.getBody().message());
            verify(authService, times(1)).loginWithGoogle("google-id-token");
        }

        @Test
        @DisplayName("Should pass token string from GoogleLoginRequest to service")
        void loginWithGoogle_passesTokenToService() {
            GoogleLoginRequest request = new GoogleLoginRequest("some-token");
            AuthResponse expected = new AuthResponse("tok", "msg");

            when(authService.loginWithGoogle("some-token")).thenReturn(expected);

            authController.loginWithGoogle(request);

            verify(authService).loginWithGoogle("some-token");
            verifyNoMoreInteractions(authService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/verify")
    class VerifyEmail {

        @Test
        @DisplayName("Should return 200 OK with AuthResponse when email verification succeeds")
        void verifyEmail_success_returns200() {
            String token = "123456";
            AuthResponse expected = new AuthResponse("jwt-verified-token", "Email verified successfully");

            when(authService.verifyEmail(token)).thenReturn(expected);

            ResponseEntity<AuthResponse> response = authController.verifyEmail(token);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("jwt-verified-token", response.getBody().token());
            assertEquals("Email verified successfully", response.getBody().message());
            verify(authService, times(1)).verifyEmail(token);
        }

        @Test
        @DisplayName("Should propagate service exceptions for invalid token")
        void verifyEmail_invalidToken_propagates() {
            String token = "000000";

            when(authService.verifyEmail(token)).thenThrow(new RuntimeException("Invalid token"));

            assertThrows(RuntimeException.class, () -> authController.verifyEmail(token));
            verify(authService).verifyEmail(token);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/request-reactivation")
    class RequestReactivation {

        @Test
        @DisplayName("Should return 200 OK with empty body when reactivation request succeeds")
        void requestReactivation_success_returns200() {
            String email = "joe@example.com";

            doNothing().when(authService).requestAccountReactivation(email);

            ResponseEntity<Void> response = authController.requestReactivation(email);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNull(response.getBody());
            verify(authService, times(1)).requestAccountReactivation(email);
        }

        @Test
        @DisplayName("Should propagate exception when user not found")
        void requestReactivation_userNotFound_propagates() {
            String email = "unknown@example.com";

            doThrow(new RuntimeException("User not found")).when(authService).requestAccountReactivation(email);

            assertThrows(RuntimeException.class, () -> authController.requestReactivation(email));
            verify(authService).requestAccountReactivation(email);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reactivate")
    class ProcessReactivation {

        @Test
        @DisplayName("Should return 200 OK with AuthResponse when reactivation succeeds")
        void processReactivation_success_returns200() {
            String token = "654321";
            AuthResponse expected = new AuthResponse("jwt-reactivated-token", "Account reactivated successfully. Welcome back!");

            when(authService.processReactivation(token)).thenReturn(expected);

            ResponseEntity<AuthResponse> response = authController.processReactivation(token);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("jwt-reactivated-token", response.getBody().token());
            assertEquals("Account reactivated successfully. Welcome back!", response.getBody().message());
            verify(authService, times(1)).processReactivation(token);
        }

        @Test
        @DisplayName("Should propagate service exceptions for invalid reactivation token")
        void processReactivation_invalidToken_propagates() {
            String token = "invalid";

            when(authService.processReactivation(token)).thenThrow(new RuntimeException("Invalid token"));

            assertThrows(RuntimeException.class, () -> authController.processReactivation(token));
            verify(authService).processReactivation(token);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("Should return 200 OK with success message map when forgot password succeeds")
        void forgotPassword_success_returns200WithMessage() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("joe@example.com");

            doNothing().when(authService).forgotPassword(request);

            ResponseEntity<Map<String, String>> response = authController.forgotPassword(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("If the email exists, a reset code was sent.", response.getBody().get("message"));
            verify(authService, times(1)).forgotPassword(request);
        }

        @Test
        @DisplayName("Should propagate service exceptions")
        void forgotPassword_serviceThrows_propagates() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("missing@example.com");

            doThrow(new RuntimeException("Not found")).when(authService).forgotPassword(request);

            assertThrows(RuntimeException.class, () -> authController.forgotPassword(request));
            verify(authService).forgotPassword(request);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("Should return 200 OK with success message map when password reset succeeds")
        void resetPassword_success_returns200WithMessage() {
            ResetPasswordRequest request = new ResetPasswordRequest("joe@example.com", "123456", "NewPass123");

            doNothing().when(authService).resetPassword(request);

            ResponseEntity<Map<String, String>> response = authController.resetPassword(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Password has been successfully reset.", response.getBody().get("message"));
            verify(authService, times(1)).resetPassword(request);
        }

        @Test
        @DisplayName("Should propagate service exceptions for invalid reset code")
        void resetPassword_invalidCode_propagates() {
            ResetPasswordRequest request = new ResetPasswordRequest("joe@example.com", "wrong", "NewPass123");

            doThrow(new RuntimeException("Invalid code")).when(authService).resetPassword(request);

            assertThrows(RuntimeException.class, () -> authController.resetPassword(request));
            verify(authService).resetPassword(request);
        }
    }
}
