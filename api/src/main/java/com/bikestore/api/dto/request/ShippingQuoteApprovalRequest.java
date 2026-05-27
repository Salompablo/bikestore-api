package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Payload used by admin to publish final shipping quote and enable payment")
public record ShippingQuoteApprovalRequest(
        @Schema(description = "Final shipping cost defined by admin", example = "15000.00")
        @NotNull(message = "Shipping cost is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Shipping cost cannot be negative")
        BigDecimal shippingCost
) {
}
