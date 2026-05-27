package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Detailed admin-facing payload representing a single order")
public record AdminOrderDetailResponse(
        @Schema(description = "Unique order ID", example = "1001")
        Long id,

        @Schema(description = "Current status of the order", example = "QUOTE_REQUESTED")
        String status,

        @Schema(description = "Delivery method (STORE_PICKUP or SHIPPING)", example = "SHIPPING")
        String deliveryMethod,

        @Schema(description = "Customer full name", example = "Ada Lovelace")
        String customerFullName,

        @Schema(description = "Customer email", example = "ada@example.com")
        String customerEmail,

        @Schema(description = "Subtotal amount before shipping", example = "1565000.00")
        BigDecimal subtotalAmount,

        @Schema(description = "Final shipping cost", example = "15000.00")
        BigDecimal shippingCost,

        @Schema(description = "Final total amount including shipping", example = "1580000.00")
        BigDecimal totalAmount,

        @Schema(description = "Date and time when the order was created", example = "2026-03-13T10:15:30")
        LocalDateTime createdAt,

        @Schema(description = "Date and time when the order was last updated", example = "2026-03-13T11:30:00")
        LocalDateTime updatedAt,

        @Schema(description = "List of items included in this order")
        List<OrderItemResponse> items,

        @Schema(description = "Delivery address (null for store pickup)", example = "Av. Colón 1234, Mar del Plata")
        String shippingAddress,

        @Schema(description = "Postal or ZIP code (null for store pickup)", example = "7600")
        String zipCode,

        @Schema(description = "Logistics tracking number", example = "AR-987654321")
        String trackingNumber,

        @Schema(description = "Contact phone number provided at checkout", example = "+5492235551234")
        String contactPhone,

        @Schema(description = "Payment status of the order", example = "PENDING")
        String paymentStatus,

        @Schema(description = "Mercado Pago preference ID when available", example = "3226905474-059535ac-abe2-4a30-97be-46cf815c92b6")
        String preferenceId,

        @Schema(description = "Mercado Pago checkout URL when available", example = "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=3226905474-059535ac-abe2-4a30-97be-46cf815c92b6")
        String checkoutUrl,

        @Schema(description = "True when order is waiting for manual shipping quote", example = "true")
        boolean requiresShippingQuote,

        @Schema(description = "True when customer can proceed with payment", example = "false")
        boolean payableNow
) {
}
