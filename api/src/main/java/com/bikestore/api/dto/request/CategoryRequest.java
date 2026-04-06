package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for creating or updating a category")
public record CategoryRequest(
        @Schema(description = "Category name", example = "Mountain Bikes")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Category description", example = "Bikes designed for off-road cycling.")
        String description,

        @Schema(description = "Default image URL for products in this category that have no images", example = "https://bucket.s3.amazonaws.com/categories/mountain-bikes.jpg")
        @Size(max = 512, message = "Default image URL must not exceed 512 characters")
        String defaultImageUrl
) {
}
