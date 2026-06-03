package com.bikestore.api.event;

import java.math.BigDecimal;
import java.util.List;

public record ShippingQuotePublishedData(
        Long orderId,
        String customerName,
        String customerEmail,
        BigDecimal productsSubtotal,
        List<ShippingQuoteItemData> items,
        BigDecimal totalAmount,
        BigDecimal shippingCost
) {
    public record ShippingQuoteItemData(
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
