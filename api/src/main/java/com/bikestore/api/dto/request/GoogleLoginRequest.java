package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for authenticating via Google")
public record GoogleLoginRequest(
        @Schema(description = "Google ID token provided by the frontend SDK", example = "eyJhbGciOiJSUzI1NiIsImtpZCI...")
        @NotBlank(message = "Google token is required")
        String token
) {
}
