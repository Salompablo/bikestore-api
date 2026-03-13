package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiNotFound;
import com.bikestore.api.annotation.ApiPublicErrors;
import com.bikestore.api.dto.request.*;
import com.bikestore.api.dto.response.AuthResponse;
import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and account recovery")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new local account and sends a 6-digit verification code to the provided email.")
    @ApiResponse(responseCode = "201", description = "User successfully registered")
    @ApiResponse(responseCode = "409", description = "Email already in use",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = """
                {
                    "status": 409,
                    "error": "Conflict",
                    "message": "Email is already in use.",
                    "timestamp": "2026-03-13T16:00:00.000Z"
                }
                """)))
    @ApiPublicErrors
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Authenticate user", description = "Validates credentials and returns a JWT token. The account must be verified.")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = """
                {
                    "status": 401,
                    "error": "Unauthorized",
                    "message": "Invalid email or password.",
                    "timestamp": "2026-03-13T16:00:00.000Z"
                }
                """)))
    @ApiResponse(responseCode = "403", description = "Account deactivated or not verified",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = """
                {
                    "status": 403,
                    "error": "Forbidden",
                    "message": "Account not verified. Please verify your email.",
                    "timestamp": "2026-03-13T16:00:00.000Z"
                }
                """)))
    @ApiPublicErrors
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Google OAuth2 Login", description = "Authenticates a user using a Google ID Token.")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiPublicErrors
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request.token()));
    }

    @Operation(summary = "Verify email", description = "Verifies a user's email using the 6-digit code.")
    @ApiResponse(responseCode = "200", description = "Email successfully verified")
    @ApiNotFound
    @ApiPublicErrors
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyEmail(
            @Parameter(description = "6-digit verification code", example = "123456", required = true)
            @RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @Operation(summary = "Request account reactivation", description = "Sends a reactivation code to a deactivated account.")
    @ApiResponse(responseCode = "200", description = "Reactivation code sent")
    @ApiNotFound
    @ApiPublicErrors
    @PostMapping("/request-reactivation")
    public ResponseEntity<Void> requestReactivation(
            @Parameter(description = "User's email", example = "pablosalompita@gmail.com", required = true)
            @RequestParam String email) {
        authService.requestAccountReactivation(email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Process reactivation", description = "Reactivates an account using the 6-digit code.")
    @ApiResponse(responseCode = "200", description = "Account successfully reactivated")
    @ApiNotFound
    @ApiPublicErrors
    @PostMapping("/reactivate")
    public ResponseEntity<AuthResponse> processReactivation(
            @Parameter(description = "6-digit reactivation code", example = "654321", required = true)
            @RequestParam String token) {
        return ResponseEntity.ok(authService.processReactivation(token));
    }

    @Operation(summary = "Forgot password", description = "Sends a password reset code to the user's email.")
    @ApiResponse(responseCode = "200", description = "Reset code sent")
    @ApiNotFound
    @ApiPublicErrors
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset code was sent."));
    }

    @Operation(summary = "Reset password", description = "Sets a new password using the 6-digit reset code.")
    @ApiResponse(responseCode = "200", description = "Password successfully reset")
    @ApiNotFound
    @ApiPublicErrors
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset."));
    }
}