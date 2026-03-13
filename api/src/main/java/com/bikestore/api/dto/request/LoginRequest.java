package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Schema(description = "User's registered email address", example = "johnmixter@gmail.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is not valid")
        String email,

        @Schema(description = "User's password", example = "MyOwnPassword")
        @NotBlank(message = "Password is required")
        String password
) {
}
