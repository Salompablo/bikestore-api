package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public response payload representing a user profile (no sensitive internal data)")
public record UserPublicResponse(
        @Schema(description = "Unique user ID", example = "1")
        Long id,

        @Schema(description = "User's first name", example = "Pablo")
        String firstName,

        @Schema(description = "User's last name", example = "Salom Pita")
        String lastName,

        @Schema(description = "User's assigned role", example = "CUSTOMER")
        String role
) {
}
