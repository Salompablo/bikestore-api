package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiCustomerErrors;
import com.bikestore.api.entity.User;
import com.bikestore.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for managing user accounts and profiles")
public class UserController {

    private final UserService userService;

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