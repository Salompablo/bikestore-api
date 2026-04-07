package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload representing a product category")
public record CategoryResponse(
        @Schema(description = "Unique category ID", example = "1")
        Long id,

        @Schema(description = "Category name", example = "Mountain Bikes")
        String name,

        @Schema(description = "Category description", example = "Bicycles designed for off-road cycling.")
        String description,

        @Schema(description = "Default image URL for products without images", example = "https://bucket.s3.amazonaws.com/categories/mountain-bikes.jpg")
        String defaultImageUrl,

        @Schema(description = "Indicates if the category is active", example = "true")
        Boolean isActive
) {
}
