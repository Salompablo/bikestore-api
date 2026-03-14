package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload representing a user profile")
public record UserResponse(
        @Schema(description = "Unique user ID", example = "1")
        Long id,

        @Schema(description = "User's email address", example = "pablosalompita@gmail.com")
        String email,

        @Schema(description = "User's first name", example = "Pablo")
        String firstName,

        @Schema(description = "User's last name", example = "Salom Pita")
        String lastName,

        @Schema(description = "User's assigned role", example = "CUSTOMER")
        String role,

        @Schema(description = "Indicates if the account is logically active", example = "true")
        Boolean isActive,

        @Schema(description = "Indicates if the user has verified their email", example = "true")
        Boolean isEmailVerified,

        @Schema(description = "Authentication provider (LOCAL or GOOGLE)", example = "LOCAL")
        String provider
) {
}