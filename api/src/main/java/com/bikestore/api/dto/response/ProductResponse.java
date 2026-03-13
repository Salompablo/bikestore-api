package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Response payload representing a product in the catalog")
public record ProductResponse(
        @Schema(description = "Unique product ID", example = "5")
        Long id,

        @Schema(description = "Stock Keeping Unit (Unique identifier)", example = "MTB-TREK-M5")
        String sku,

        @Schema(description = "Product name", example = "Trek Marlin 5")
        String name,

        @Schema(description = "Detailed description", example = "Great mountain bike for beginners.")
        String description,

        @Schema(description = "Current price", example = "850000.00")
        BigDecimal price,

        @Schema(description = "Available units in stock", example = "10")
        Integer stock,

        @Schema(description = "Category this product belongs to")
        CategoryResponse category,

        @Schema(description = "List of S3 image URLs", example = "[\"https://bucket.s3.amazonaws.com/image1.jpg\"]")
        List<String> images,

        @Schema(description = "Indicates if the product is active and visible to customers", example = "true")
        Boolean isActive,

        @Schema(description = "Weight in kilograms", example = "14.5")
        Double weight,

        @Schema(description = "Length in centimeters", example = "180.0")
        Double length,

        @Schema(description = "Width in centimeters", example = "20.0")
        Double width,

        @Schema(description = "Height in centimeters", example = "105.0")
        Double height
) {
}
