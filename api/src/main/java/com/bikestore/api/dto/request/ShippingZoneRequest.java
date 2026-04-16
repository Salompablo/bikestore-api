package com.bikestore.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request payload for creating or updating a shipping zone")
public record ShippingZoneRequest(
        @Schema(description = "Descriptive name for the zone", example = "Mar del Plata Local")
        @NotBlank(message = "Zone name is required")
        String name,

        @Schema(description = "ZIP code prefix that matches this zone", example = "7600")
        @NotBlank(message = "ZIP prefix is required")
        String zipPrefix,

        @Schema(description = "Shipping cost for this zone", example = "2500.00")
        @NotNull(message = "Cost is required")
        @Positive(message = "Cost must be positive")
        BigDecimal cost,

        @Schema(description = "Estimated delivery days", example = "1")
        @NotNull(message = "Estimated days is required")
        @Positive(message = "Estimated days must be positive")
        Integer estimatedDays,

        @Schema(description = "Logistics provider name", example = "Envío gestionado por Bikes Asaro")
        String provider
) {
}
