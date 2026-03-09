package com.bikestore.api.dto.response;

import java.math.BigDecimal;

public record ShippingQuoteResponse(
        String provider,
        BigDecimal cost,
        Integer estimatedDays
) {
}
