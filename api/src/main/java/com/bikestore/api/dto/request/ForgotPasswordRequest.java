package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for requesting a password reset code")
public record ForgotPasswordRequest(
        @Schema(description = "User's registered email address", example = "joeluani87@gmail.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email
) {
}
