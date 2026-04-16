package com.bikestore.api.dto.request;

import com.bikestore.api.entity.enums.DeliveryMethod;
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

        @Schema(description = "Delivery method chosen by the buyer", example = "STORE_PICKUP")
        @NotNull(message = "Delivery method is required (STORE_PICKUP or SHIPPING)")
        DeliveryMethod deliveryMethod,

        @Schema(description = "Shipping address for delivery (required when deliveryMethod is SHIPPING)", example = "Av. Colón 1234, Mar del Plata")
        String shippingAddress,

        @Schema(description = "Postal or ZIP code (required when deliveryMethod is SHIPPING)", example = "7600")
        String zipCode,

        @Schema(description = "Calculated shipping cost (required when deliveryMethod is SHIPPING, ignored for STORE_PICKUP)", example = "15000.00")
        @Min(value = 0, message = "Shipping cost cannot be negative")
        BigDecimal shippingCost
) {
}
