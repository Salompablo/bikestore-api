package com.bikestore.api.event;

import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderStatusUpdatedData(
        Long orderId,
        String customerName,
        String customerEmail,
        OrderStatus newStatus,
        DeliveryMethod deliveryMethod,
        BigDecimal totalAmount,
        List<String> productPreviewImages
) {
}
