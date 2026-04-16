package com.bikestore.api.service;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    Page<OrderResponse> getMyOrders(User authenticatedUser, Pageable pageable);
    Page<OrderResponse> getAllOrders(Pageable pageable);
    OrderResponse updateOrderStatus(Long id, OrderStatus newStatus);
    Order createPendingOrder(CheckoutRequest checkoutRequest, User authenticatedUser);
    void updateOrderPreference(Long orderId, String preferenceId);
    void confirmOrder(Long orderId);
}
