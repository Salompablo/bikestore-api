package com.bikestore.api.event;

public record ShippingQuoteRequestedData(
        Long orderId,
        String customerName,
        String customerEmail,
        String contactPhone,
        String shippingAddress,
        String zipCode
) {
}
