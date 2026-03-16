package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Payload for processing a shopping cart order")
public record CheckoutRequest(
        @Schema(description = "List of items to purchase")
        @NotEmpty(message = "Cart cannot be empty")
        @Valid
        List<CartItemRequest> items,

        @Schema(description = "Shipping address for delivery", example = "Av. Colón 1234, Mar del Plata")
        String shippingAddress,

        @Schema(description = "Postal or ZIP code", example = "7600")
        String zipCode,

        @Schema(description = "Calculated shipping cost (0 if picking up at store)", example = "15000.00")
        @NotNull(message = "Shipping cost is required. Send 0 for store pickup.")
        @Min(value = 0, message = "Shipping cost cannot be negative")
        BigDecimal shippingCost
) {
}
