package com.bikestore.api.service.impl;

import com.bikestore.api.dto.request.CartItemRequest;
import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.OrderItemResponse;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.OrderMapper;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.UserRepository;
import com.bikestore.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return orderRepository.findByUserId(user.getId(), pageable)
                .map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public Order createPendingOrder(CheckoutRequest checkoutRequest) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        Map<Long, Integer> consolidatedCart = checkoutRequest.items().stream()
                .collect(Collectors.groupingBy(
                        CartItemRequest::productId,
                        Collectors.summingInt(CartItemRequest::quantity)
                ));

        for (Map.Entry<Long, Integer> entry : consolidatedCart.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            int updatedRows = productRepository.deductStock(productId, quantity);

            if (updatedRows == 0) {
                throw new ConflictException("Not enough stock remaining for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(product.getPrice());
            order.addItem(orderItem);

            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(quantity)));
        }

        BigDecimal shippingCost = checkoutRequest.shippingCost() != null ? checkoutRequest.shippingCost() : BigDecimal.ZERO;
        if (shippingCost.compareTo(BigDecimal.ZERO) > 0) {
            order.setShippingCost(shippingCost);
            order.setShippingAddress(checkoutRequest.shippingAddress());
            order.setZipCode(checkoutRequest.zipCode());
            totalAmount = totalAmount.add(shippingCost);
        } else {
            order.setShippingCost(BigDecimal.ZERO);
            order.setShippingAddress("Retiro en sucursal");
            order.setZipCode("7600");
        }

        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void updateOrderPreference(Long orderId, String preferenceId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setPreferenceId(preferenceId);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is already {}. Skipping webhook confirmation.", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        log.info("Order {} confirmed as PAID.", orderId);
    }
}
