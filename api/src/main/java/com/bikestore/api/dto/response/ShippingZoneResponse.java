package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Response payload representing a shipping zone")
public record ShippingZoneResponse(
        @Schema(description = "Unique shipping zone ID", example = "1")
        Long id,

        @Schema(description = "Descriptive name for the zone", example = "Mar del Plata Local")
        String name,

        @Schema(description = "ZIP code prefix that matches this zone", example = "7600")
        String zipPrefix,

        @Schema(description = "Shipping cost for this zone", example = "2500.00")
        BigDecimal cost,

        @Schema(description = "Estimated delivery days", example = "1")
        Integer estimatedDays,

        @Schema(description = "Logistics provider name", example = "Envío gestionado por Bikes Asaro")
        String provider
) {
}
