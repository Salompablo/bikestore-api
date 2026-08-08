package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current account verification state")
public record AccountStatusResponse(
        @Schema(description = "Whether the account is currently active", example = "true")
        boolean isActive,

        @Schema(description = "Whether the account email is already verified", example = "false")
        boolean isEmailVerified,

        @Schema(description = "Whether the account is pending verification", example = "true")
        boolean pendingVerification
) {
}
