package com.bikestore.api.dto.request;

import com.bikestore.api.entity.enums.ContactTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Payload for submitting a contact form message")
public record ContactRequest(

        @Schema(description = "Full name of the sender", example = "John Doe")
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Schema(description = "Email address of the sender", example = "john@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Schema(description = "Optional phone number of the sender", example = "+5492235551234")
        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Schema(description = "Optional related order ID", example = "42")
        Long orderId,

        @Schema(description = "Topic that best describes the inquiry", example = "ORDER_ISSUE")
        @NotNull(message = "Topic is required")
        ContactTopic topic,

        @Schema(description = "Message body (max 1000 characters)", example = "I have a question about my order...")
        @NotBlank(message = "Message is required")
        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        String message,

        @Schema(description = "Google reCAPTCHA v3 token obtained from the frontend", example = "03AGdBq2...")
        @NotBlank(message = "reCAPTCHA token is required")
        String captchaToken
) {
}
