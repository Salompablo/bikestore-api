package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for registering a new user")
public record RegisterRequest(
        @Schema(description = "User's first name", example = "Joe")
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(description = "User's last name", example = "Luani")
        @NotBlank(message = "Last name is required")
        String lastName,

        @Schema(description = "User's email address", example = "joeluani87@gmail.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is not valid")
        String email,

        @Schema(description = "User's password", example = "MyOwnPassword")
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 15, message = "Password must be between 6 and 15 characters long")
        String password
) {
}
