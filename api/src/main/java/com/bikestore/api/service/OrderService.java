package com.bikestore.api.service;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.AdminOrderDetailResponse;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface OrderService {
    Page<OrderResponse> getMyOrders(User authenticatedUser, Pageable pageable);
    OrderResponse getMyOrderById(Long id, User authenticatedUser);
    Page<OrderResponse> getAllOrders(Pageable pageable);
    AdminOrderDetailResponse getOrderByIdForAdmin(Long id);
    OrderResponse updateOrderStatus(Long id, OrderStatus newStatus);
    Order createPendingOrder(CheckoutRequest checkoutRequest, User authenticatedUser);
    Order createPendingShippingOrderAndNotify(CheckoutRequest checkoutRequest, User authenticatedUser);
    void updateOrderPreference(Long orderId, String preferenceId);
    Order prepareShippingQuote(Long orderId, BigDecimal shippingCost);
    void confirmOrder(Long orderId);
    void markOrderAsPending(Long orderId);
    void cancelOrder(Long orderId, User authenticatedUser);
}
