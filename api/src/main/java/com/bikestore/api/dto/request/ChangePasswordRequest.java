package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for changing the authenticated user's password")
public record ChangePasswordRequest(

        @Schema(description = "The user's current password", example = "OldPassword1")
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(description = "The new password to set (minimum 8 characters)", example = "NewPassword1")
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters long")
        String newPassword
) {
}
