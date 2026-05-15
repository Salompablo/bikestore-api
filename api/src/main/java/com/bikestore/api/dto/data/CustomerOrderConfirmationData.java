package com.bikestore.api.dto.data;

import com.bikestore.api.entity.enums.DeliveryMethod;

import java.math.BigDecimal;
import java.util.List;

public record CustomerOrderConfirmationData(
        Long orderId,
        String customerName,
        String customerEmail,
        List<String> productPreviewImages,
        BigDecimal totalAmount,
        DeliveryMethod deliveryMethod,
        String shippingAddress,
        String zipCode
) {
}
