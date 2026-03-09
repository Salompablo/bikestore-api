package com.bikestore.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CartRequest(
        @NotEmpty(message = "Cart cannot be empty")
        @Valid
        List<CartItemRequest> items,
        String shippingAddress,
        String zipCode,
        @NotNull(message = "Shipping cost is required. Send 0 for store pickup.")
        @Min(value = 0, message = "Shipping cost cannot be negative")
        BigDecimal shippingCost
) {
}
