package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiCustomerErrors;
import com.bikestore.api.dto.request.ChangePasswordRequest;
import com.bikestore.api.dto.request.UpdateProfileRequest;
import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.dto.response.MessageResponse;
import com.bikestore.api.dto.response.UserResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for managing user accounts and profiles")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get my profile", description = "Retrieves the authenticated user's own profile information.")
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))
    @ApiCustomerErrors
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getMyProfile(user));
    }

    @Operation(summary = "Update my profile", description = "Updates the authenticated user's first name, last name, and default phone number.")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))
    @ApiCustomerErrors
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(user, request));
    }

    @Operation(summary = "Change my password", description = "Changes the authenticated user's password. Only available for accounts registered with email and password (not social login).")
    @ApiResponse(responseCode = "200", description = "Password changed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflict - Current password is incorrect or account uses social login",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = """
                            {
                                "status": 409,
                                "error": "Conflict",
                                "message": "Current password is incorrect",
                                "timestamp": "2026-03-13T16:00:00.000Z"
                            }
                            """)))
    @ApiCustomerErrors
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changeMyPassword(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user, request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }

    @Operation(summary = "Deactivate my account", description = "Logically deletes the authenticated user's account. Requires CUSTOMER or ADMIN privileges.")
    @ApiResponse(responseCode = "204", description = "Account successfully deactivated")
    @ApiCustomerErrors
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMyAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        userService.deactivateUser(user);
        return ResponseEntity.noContent().build();
    }
}