package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload after successful authentication or registration")
public record AuthResponse(
        @Schema(description = "JWT Token for accessing protected endpoints", example = "eyJhbGciOiJIUzI1NiIsInR5cCI...")
        String token,

        @Schema(description = "Informational message about the action performed", example = "Authentication successful")
        String message
) {
}
