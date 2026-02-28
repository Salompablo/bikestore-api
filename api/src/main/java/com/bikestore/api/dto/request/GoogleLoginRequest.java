package com.bikestore.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "Google token is required")
        String token
) {
}
