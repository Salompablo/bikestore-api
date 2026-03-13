package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for creating or updating a category")
public record CategoryRequest(
        @Schema(description = "Category name", example = "Mountain Bikes")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Category description", example = "Bikes designed for off-road cycling.")
        String description
) {
}
