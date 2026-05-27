package com.bikestore.api.event;

import java.math.BigDecimal;

public record ShippingQuotePublishedData(
        Long orderId,
        String customerName,
        String customerEmail,
        BigDecimal totalAmount,
        BigDecimal shippingCost
) {
}
