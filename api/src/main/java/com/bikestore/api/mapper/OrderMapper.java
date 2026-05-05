package com.bikestore.api.mapper;

import com.bikestore.api.dto.response.OrderItemResponse;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getDeliveryMethod().name(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses,
                order.getShippingAddress(),
                order.getZipCode(),
                order.getShippingCost(),
                order.getTrackingNumber(),
                order.getContactPhone()
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null || item.getProduct() == null) {
            return null;
        }

        List<String> images = item.getProduct().getImages();
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                images.isEmpty() ? null : images.get(0)
        );
    }
}
