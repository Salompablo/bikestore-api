package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload containing a user email")
public record EmailRequest(
        @Schema(description = "User's registered email address", example = "joeluani87@gmail.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email
) {
    public EmailRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
