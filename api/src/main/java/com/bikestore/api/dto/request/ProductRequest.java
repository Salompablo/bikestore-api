package com.bikestore.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank(message = "SKU is required")
        String sku,

        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        BigDecimal price,

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock cannot be negative")
        Integer stock,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        List<String> images,

        @NotNull(message = "Weight is required")
        @Min(0)
        Double weight,

        @NotNull(message = "Length is required")
        @Min(0)
        Double length,

        @NotNull(message = "Width is required")
        @Min(0)
        Double width,

        @NotNull(message = "Height is required")
        @Min(0)
        Double height
) {
}
