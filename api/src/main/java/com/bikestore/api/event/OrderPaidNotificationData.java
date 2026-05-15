package com.bikestore.api.event;

import com.bikestore.api.entity.enums.DeliveryMethod;

import java.math.BigDecimal;
import java.util.List;

public record OrderPaidNotificationData(
        Long orderId,
        String customerFullName,
        String customerEmail,
        String contactPhone,
        List<OrderPaidItemData> items,
        BigDecimal totalAmount,
        DeliveryMethod deliveryMethod
) {
    public record OrderPaidItemData(
            String productName,
            Integer quantity
    ) {
    }
}
