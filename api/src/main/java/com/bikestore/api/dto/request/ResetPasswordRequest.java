package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for resetting a forgotten password")
public record ResetPasswordRequest(
        @Schema(description = "User's email address", example = "joeluani87@gmail.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,

        @Schema(description = "6-digit verification code sent to the email", example = "321452")
        @NotBlank(message = "Code is required")
        String code,

        @Schema(description = "New password to set", example = "MyNewOwnPassword")
        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 15, message = "Password must be between 6 and 15 characters long")
        String newPassword
) {
}
