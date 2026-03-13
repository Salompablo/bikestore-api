package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Individual item within a shopping cart")
public record CartItemRequest(
        @Schema(description = "ID of the product being purchased", example = "5")
        @NotNull(message = "Product ID is required")
        Long productId,

        @Schema(description = "Quantity of the product", example = "2")
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}
