package com.bikestore.api.service.impl;

import com.bikestore.api.dto.request.CartItemRequest;
import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.OrderItemResponse;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.StockReservation;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.entity.enums.ReservationStatus;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.OrderMapper;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.StockReservationRepository;
import com.bikestore.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;

    private static final int RESERVATION_TTL_MINUTES = 10;
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = EnumSet.of(OrderStatus.INITIATED, OrderStatus.PENDING);
    private static final Set<OrderStatus> HIDDEN_FROM_USER = EnumSet.of(OrderStatus.INITIATED);

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(User authenticatedUser, Pageable pageable) {
        return orderRepository.findByUserIdAndStatusNotIn(authenticatedUser.getId(), HIDDEN_FROM_USER, pageable)
                .map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(Long id, User authenticatedUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (!order.getUser().getId().equals(authenticatedUser.getId())) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }

        if (HIDDEN_FROM_USER.contains(order.getStatus())) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }

        return orderMapper.toOrderResponse(order);
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

        validateStatusTransition(order, newStatus);

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public Order createPendingOrder(CheckoutRequest checkoutRequest, User authenticatedUser) {
        DeliveryMethod deliveryMethod = checkoutRequest.deliveryMethod();

        if (deliveryMethod == DeliveryMethod.SHIPPING) {
            if (checkoutRequest.shippingAddress() == null || checkoutRequest.shippingAddress().isBlank()) {
                throw new IllegalArgumentException("Shipping address is required for delivery orders");
            }
            if (checkoutRequest.zipCode() == null || checkoutRequest.zipCode().isBlank()) {
                throw new IllegalArgumentException("ZIP code is required for delivery orders");
            }
            if (checkoutRequest.shippingCost() == null || checkoutRequest.shippingCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("A valid shipping cost is required for delivery orders");
            }
        }

        // Cancel any pre-existing active order for this user before creating a new one
        List<Order> activeOrders = orderRepository.findByUserIdAndStatusIn(
                authenticatedUser.getId(), CANCELLABLE_STATUSES);
        for (Order activeOrder : activeOrders) {
            log.info("Auto-cancelling existing active order {} for user {} before creating a new one.",
                    activeOrder.getId(), authenticatedUser.getId());
            internalCancelOrder(activeOrder);
        }

        Order order = new Order();
        order.setUser(authenticatedUser);
        order.setStatus(OrderStatus.INITIATED);
        order.setDeliveryMethod(deliveryMethod);

        BigDecimal totalAmount = BigDecimal.ZERO;

        Map<Long, Integer> consolidatedCart = checkoutRequest.items().stream()
                .collect(Collectors.groupingBy(
                        CartItemRequest::productId,
                        Collectors.summingInt(CartItemRequest::quantity)
                ));

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES);
        List<StockReservation> reservations = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : consolidatedCart.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            int updatedRows = productRepository.reserveStock(productId, quantity);

            if (updatedRows == 0) {
                throw new ConflictException(
                        "Not enough stock for: " + product.getName(),
                        "RESERVED_TEMPORARILY",
                        RESERVATION_TTL_MINUTES * 60
                );
            }

            reservations.add(StockReservation.builder()
                    .product(product)
                    .quantity(quantity)
                    .status(ReservationStatus.ACTIVE)
                    .expiresAt(expiresAt)
                    .build());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(product.getPrice());
            order.addItem(orderItem);

            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(quantity)));
        }

        if (deliveryMethod == DeliveryMethod.SHIPPING) {
            BigDecimal shippingCost = checkoutRequest.shippingCost();
            order.setShippingCost(shippingCost);
            order.setShippingAddress(checkoutRequest.shippingAddress());
            order.setZipCode(checkoutRequest.zipCode());
            totalAmount = totalAmount.add(shippingCost);
        } else {
            order.setShippingCost(BigDecimal.ZERO);
            order.setShippingAddress(null);
            order.setZipCode(null);
        }

        order.setTotalAmount(totalAmount);
        order.setContactPhone(checkoutRequest.contactPhone());
        Order savedOrder = orderRepository.save(order);

        for (StockReservation reservation : reservations) {
            reservation.setOrder(savedOrder);
            stockReservationRepository.save(reservation);
        }

        log.info("Order {} created in INITIATED state with {} reservation(s), expires at {}",
                savedOrder.getId(), consolidatedCart.size(), expiresAt);
        return savedOrder;
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
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.INITIATED && order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is already {}. Skipping webhook confirmation.", orderId, order.getStatus());
            return;
        }

        List<StockReservation> activeReservations =
                stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);

        for (StockReservation reservation : activeReservations) {
            Long productId = reservation.getProduct().getId();
            Integer quantity = reservation.getQuantity();

            int updated = productRepository.deductAndReleaseStock(productId, quantity);
            if (updated == 0) {
                log.error("Failed to deduct stock for product {} (qty {}) on order {}. Possible data inconsistency.",
                        productId, quantity, orderId);
            }

            reservation.setStatus(ReservationStatus.CONSUMED);
            stockReservationRepository.save(reservation);
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        log.info("Order {} confirmed as PAID. {} reservation(s) consumed.", orderId, activeReservations.size());
    }

    @Override
    @Transactional
    public void markOrderAsPending(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PENDING) {
            log.info("Order {} is already PENDING. Skipping.", orderId);
            return;
        }

        if (order.getStatus() != OrderStatus.INITIATED) {
            log.warn("Order {} is in status {}. Cannot transition to PENDING.", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        log.info("Order {} transitioned from INITIATED to PENDING.", orderId);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, User authenticatedUser) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(authenticatedUser.getId())) {
            throw new ConflictException("Order does not belong to the authenticated user");
        }

        // Idempotency: already cancelled → no-op (supports double-DELETE safely)
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Order {} already cancelled, no-op.", orderId);
            return;
        }

        if (order.getStatus() != OrderStatus.INITIATED && order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(
                    "Cannot cancel order in status: " + order.getStatus());
        }

        internalCancelOrder(order);
        log.info("Order {} cancelled by user {}.", orderId, authenticatedUser.getId());
    }

    /**
     * Releases all ACTIVE stock reservations for the given order and marks the order as CANCELLED.
     * If releasing reserved stock for a reservation fails (concurrent update), a warning is logged
     * and processing continues with the remaining reservations.
     * Must be called within an active transaction.
     */
    private void internalCancelOrder(Order order) {
        List<StockReservation> activeReservations =
                stockReservationRepository.findByOrderIdAndStatus(order.getId(), ReservationStatus.ACTIVE);

        for (StockReservation reservation : activeReservations) {
            int updated = productRepository.releaseReservedStock(
                    reservation.getProduct().getId(), reservation.getQuantity());
            if (updated == 0) {
                log.warn("Could not release reservedStock for product id={} (qty {}). Possible data inconsistency.",
                        reservation.getProduct().getId(), reservation.getQuantity());
            }
            reservation.setStatus(ReservationStatus.EXPIRED);
            stockReservationRepository.save(reservation);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order {} cancelled. {} reservation(s) released.", order.getId(), activeReservations.size());
    }

    private void validateStatusTransition(Order order, OrderStatus newStatus) {
        OrderStatus current = order.getStatus();
        DeliveryMethod method = order.getDeliveryMethod();

        // CANCELLED is always allowed from any state
        if (newStatus == OrderStatus.CANCELLED) {
            return;
        }

        Set<OrderStatus> allowed;

        if (method == DeliveryMethod.STORE_PICKUP) {
            allowed = switch (current) {
                case PAID -> EnumSet.of(OrderStatus.READY_FOR_PICKUP);
                case READY_FOR_PICKUP -> EnumSet.of(OrderStatus.PICKED_UP);
                default -> EnumSet.noneOf(OrderStatus.class);
            };
        } else {
            allowed = switch (current) {
                case PAID -> EnumSet.of(OrderStatus.SHIPPED);
                case SHIPPED -> EnumSet.of(OrderStatus.DELIVERED);
                default -> EnumSet.noneOf(OrderStatus.class);
            };
        }

        if (!allowed.contains(newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Cannot transition order %d from %s to %s (delivery method: %s)",
                            order.getId(), current, newStatus, method));
        }
    }
}
