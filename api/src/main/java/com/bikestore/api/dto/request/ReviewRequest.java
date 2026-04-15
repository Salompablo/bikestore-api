package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for creating or updating a review")
public record ReviewRequest(
        @Schema(description = "Rating from 1 to 5", example = "4")
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @Schema(description = "Optional comment about the product", example = "Great bike, very comfortable!")
        String comment,

        @Schema(description = "ID of the product being reviewed", example = "1")
        @NotNull(message = "Product ID is required")
        Long productId
) {
}
