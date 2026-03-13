package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Response payload containing a shipping quote")
public record ShippingQuoteResponse(
        @Schema(description = "Name of the logistics provider", example = "Andreani")
        String provider,

        @Schema(description = "Calculated shipping cost", example = "15000.00")
        BigDecimal cost,

        @Schema(description = "Estimated delivery time in days", example = "3")
        Integer estimatedDays
) {
}
