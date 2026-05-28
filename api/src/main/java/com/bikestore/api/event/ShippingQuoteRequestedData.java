package com.bikestore.api.event;

import java.math.BigDecimal;
import java.util.List;

public record ShippingQuoteRequestedData(
        Long orderId,
        String customerName,
        String customerEmail,
        String contactPhone,
        String shippingAddress,
        String zipCode,
        BigDecimal productsSubtotal,
        List<ShippingQuoteItemData> items
) {
    public record ShippingQuoteItemData(
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
