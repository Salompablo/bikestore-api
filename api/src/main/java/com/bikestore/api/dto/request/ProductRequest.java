package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Payload for creating or updating a product")
public record ProductRequest(
        @Schema(description = "Stock Keeping Unit (Unique identifier)", example = "MTB-TREK-M5")
        @NotBlank(message = "SKU is required")
        String sku,

        @Schema(description = "Product name", example = "Trek Marlin 5")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Detailed description of the product", example = "Great mountain bike for beginners.")
        @NotBlank(message = "Description is required")
        @Size(min = 10, message = "Description must be at least 10 characters long")
        String description,

        @Schema(description = "Product price in local currency", example = "850000.00")
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        BigDecimal price,

        @Schema(description = "Available units in stock", example = "10")
        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock cannot be negative")
        Integer stock,

        @Schema(description = "ID of the category this product belongs to", example = "1")
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @Schema(description = "List of image URLs from S3", example = "[\"https://bucket.s3.amazonaws.com/image1.jpg\"]")
        List<String> images,

        @Schema(description = "Weight in kilograms", example = "14.5")
        @Min(0)
        Double weight,

        @Schema(description = "Length in centimeters", example = "180.0")
        @Min(0)
        Double length,

        @Schema(description = "Width in centimeters", example = "20.0")
        @Min(0)
        Double width,

        @Schema(description = "Height in centimeters", example = "105.0")
        @Min(0)
        Double height
) {
}
