package com.bikestore.api.mapper;

import com.bikestore.api.dto.response.AdminOrderDetailResponse;
import com.bikestore.api.dto.response.OrderItemResponse;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    private static final String MERCADO_PAGO_CHECKOUT_BASE_URL =
            "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=";

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getDeliveryMethod().name(),
                calculateSubtotal(order),
                order.getTotalAmount(),
                order.getCreatedAt(),
                toOrderItemResponses(order),
                order.getShippingAddress(),
                order.getZipCode(),
                order.getShippingCost(),
                order.getTrackingNumber(),
                order.getContactPhone(),
                order.getPaymentStatus().name(),
                order.getPreferenceId(),
                buildCheckoutUrl(order.getPreferenceId()),
                requiresShippingQuote(order),
                isPayableNow(order)
        );
    }

    public AdminOrderDetailResponse toAdminOrderDetailResponse(Order order) {
        if (order == null) {
            return null;
        }

        return new AdminOrderDetailResponse(
                order.getId(),
                order.getStatus().name(),
                order.getDeliveryMethod().name(),
                buildCustomerFullName(order),
                order.getUser() == null ? null : order.getUser().getEmail(),
                calculateSubtotal(order),
                order.getShippingCost(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                toOrderItemResponses(order),
                order.getShippingAddress(),
                order.getZipCode(),
                order.getTrackingNumber(),
                order.getContactPhone(),
                order.getPaymentStatus().name(),
                order.getPreferenceId(),
                buildCheckoutUrl(order.getPreferenceId()),
                requiresShippingQuote(order),
                isPayableNow(order)
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

    private List<OrderItemResponse> toOrderItemResponses(Order order) {
        return order.getItems().stream()
                .map(this::toOrderItemResponse)
                .toList();
    }

    private BigDecimal calculateSubtotal(Order order) {
        return order.getItems().stream()
                .filter(item -> item != null && item.getUnitPrice() != null && item.getQuantity() != null)
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean requiresShippingQuote(Order order) {
        return order.getStatus() == OrderStatus.QUOTE_REQUESTED;
    }

    private boolean isPayableNow(Order order) {
        return order.getPreferenceId() != null && !order.getPreferenceId().isBlank();
    }

    private String buildCheckoutUrl(String preferenceId) {
        if (preferenceId == null || preferenceId.isBlank()) {
            return null;
        }
        return MERCADO_PAGO_CHECKOUT_BASE_URL + preferenceId.trim();
    }

    private String buildCustomerFullName(Order order) {
        if (order.getUser() == null) {
            return null;
        }

        String firstName = order.getUser().getFirstName() == null ? "" : order.getUser().getFirstName().trim();
        String lastName = order.getUser().getLastName() == null ? "" : order.getUser().getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "Cliente" : fullName;
    }
}
