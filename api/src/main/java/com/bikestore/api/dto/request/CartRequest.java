package com.bikestore.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CartRequest(
        @NotEmpty(message = "Cart cannot be empty")
        @Valid
        List<CartItemRequest> items
) {
}
