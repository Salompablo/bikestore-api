package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for updating the authenticated user's profile")
public record UpdateProfileRequest(

        @Schema(description = "User's first name", example = "Pablo")
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Schema(description = "User's last name", example = "Salom")
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Schema(description = "User's default contact phone number", example = "+5492235551234")
        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone
) {
        public UpdateProfileRequest {
                if (firstName != null) {
                        firstName = firstName.trim();
                }
                if (lastName != null) {
                        lastName = lastName.trim();
                }
                if (phone != null) {
                        phone = phone.trim();
                }
        }
}
