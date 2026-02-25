package com.bikestore.api.service.impl;

import com.bikestore.api.dto.response.OrderItemResponse;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.UserRepository;
import com.bikestore.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return orderRepository.findByUserId(user.getId(), pageable)
                .map(order -> new OrderResponse(
                        order.getId(),
                        order.getStatus().name(),
                        order.getTotalAmount(),
                        order.getCreatedAt(),
                        order.getItems().stream()
                                .map(item -> new OrderItemResponse(
                                        item.getProduct().getId(),
                                        item.getProduct().getName(),
                                        item.getQuantity(),
                                        item.getUnitPrice()
                                )).toList()
                ));
    }
}
