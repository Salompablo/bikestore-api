package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Response payload representing a customer's order")
public record OrderResponse(
        @Schema(description = "Unique order ID", example = "1001")
        Long id,

        @Schema(description = "Current status of the order (e.g., PENDING, PAID, SHIPPED, CANCELLED)", example = "PAID")
        String status,

        @Schema(description = "Delivery method (STORE_PICKUP or SHIPPING)", example = "SHIPPING")
        String deliveryMethod,

        @Schema(description = "Total amount of the order including shipping", example = "1715000.00")
        BigDecimal totalAmount,

        @Schema(description = "Date and time when the order was created", example = "2026-03-13T10:15:30.000Z")
        LocalDateTime createdAt,

        @Schema(description = "List of items included in this order")
        List<OrderItemResponse> items,

        @Schema(description = "Delivery address (null for store pickup)", example = "Av. Colón 1234, Mar del Plata")
        String shippingAddress,

        @Schema(description = "Postal or ZIP code (null for store pickup)", example = "7600")
        String zipCode,

        @Schema(description = "Cost of shipping (0 for store pickup)", example = "15000.00")
        BigDecimal shippingCost,

        @Schema(description = "Logistics tracking number", example = "AR-987654321")
        String trackingNumber,

        @Schema(description = "Contact phone number provided at checkout", example = "+5492235551234")
        String contactPhone
) {
}
