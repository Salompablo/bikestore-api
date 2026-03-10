package com.bikestore.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Code is required")
        String code,

        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 15, message = "Password must be between 6 and 15 characters long")
        String newPassword
) {
}
