package com.bikestore.api.controller;

import com.bikestore.api.entity.User;
import com.bikestore.api.service.UserService;
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
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMyAccount(@AuthenticationPrincipal User user) {
        userService.deactivateUser(user);
        return ResponseEntity.noContent().build();
    }
}
