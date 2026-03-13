package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Response payload representing a single item within an order")
public record OrderItemResponse(
        @Schema(description = "ID of the purchased product", example = "5")
        Long productId,

        @Schema(description = "Name of the purchased product at the time of order", example = "Trek Marlin 5")
        String productName,

        @Schema(description = "Quantity purchased", example = "2")
        Integer quantity,

        @Schema(description = "Price per unit at the time of purchase", example = "850000.00")
        BigDecimal unitPrice
) {
}
