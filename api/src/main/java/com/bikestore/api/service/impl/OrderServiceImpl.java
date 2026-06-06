package com.bikestore.api.service.impl;

import com.bikestore.api.dto.data.CustomerOrderConfirmationData;
import com.bikestore.api.dto.request.CartItemRequest;
import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.AdminOrderDetailResponse;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.StockReservation;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.entity.enums.PaymentStatus;
import com.bikestore.api.entity.enums.ReservationStatus;
import com.bikestore.api.event.AdminOrderNotificationEvent;
import com.bikestore.api.event.CustomerOrderConfirmationEvent;
import com.bikestore.api.event.OrderPaidNotificationData;
import com.bikestore.api.event.OrderStatusUpdatedData;
import com.bikestore.api.event.OrderStatusUpdatedEvent;
import com.bikestore.api.event.ShippingQuoteRequestedData;
import com.bikestore.api.event.ShippingQuoteRequestedEvent;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.OrderMapper;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.StockReservationRepository;
import com.bikestore.api.service.OrderService;
import com.bikestore.api.service.OrderStatusTransitionPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderStatusTransitionPolicy transitionPolicy;

    @Value("${app.orders.reservation-ttl.pickup-minutes:10}")
    private int pickupReservationTtlMinutes;

    @Value("${app.orders.reservation-ttl.shipping-quote-hours:24}")
    private int shippingQuoteReservationTtlHours;

    @Value("${app.orders.reservation-ttl.quote-ready-hours:2}")
    private int quoteReadyTtlHours;

    private static final Set<OrderStatus> CANCELLABLE_STATUSES = EnumSet.of(
            OrderStatus.INITIATED,
            OrderStatus.PENDING,
            OrderStatus.QUOTE_REQUESTED,
            OrderStatus.QUOTE_READY_PAYMENT_PENDING
    );
    private static final Set<OrderStatus> NOTIFIABLE_STATUSES = EnumSet.of(
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.SHIPPED
    );
    private static final Set<OrderStatus> HIDDEN_FROM_USER = EnumSet.of(OrderStatus.INITIATED);
    private static final int SHIPPING_ADDRESS_MIN_LENGTH = 5;
    private static final int SHIPPING_ADDRESS_MAX_LENGTH = 200;
    private static final Pattern SHIPPING_ADDRESS_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s.,#°'()/-]+$");
    private static final Pattern ZIP_CODE_PATTERN = Pattern.compile("^(?:\\d{4}|[A-Z]\\d{4}[A-Z]{3})$");

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
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrderByIdForAdmin(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return orderMapper.toAdminOrderDetailResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        validateStatusTransition(order, newStatus);

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        if (NOTIFIABLE_STATUSES.contains(newStatus)) {
            eventPublisher.publishEvent(
                    new OrderStatusUpdatedEvent(this, buildOrderStatusUpdatedData(updatedOrder)));
        }

        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public Order createPendingOrder(CheckoutRequest checkoutRequest, User authenticatedUser) {
        DeliveryMethod deliveryMethod = checkoutRequest.deliveryMethod();
        String normalizedShippingAddress = null;
        String normalizedZipCode = null;

        if (deliveryMethod == DeliveryMethod.SHIPPING) {
            normalizedShippingAddress = normalizeShippingAddress(checkoutRequest.shippingAddress());
            normalizedZipCode = normalizeZipCode(checkoutRequest.zipCode());
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
        order.setStatus(deliveryMethod == DeliveryMethod.SHIPPING ? OrderStatus.QUOTE_REQUESTED : OrderStatus.INITIATED);
        order.setDeliveryMethod(deliveryMethod);

        BigDecimal totalAmount = BigDecimal.ZERO;

        Map<Long, Integer> consolidatedCart = checkoutRequest.items().stream()
                .collect(Collectors.groupingBy(
                        CartItemRequest::productId,
                        Collectors.summingInt(CartItemRequest::quantity)
                ));

        LocalDateTime expiresAt = deliveryMethod == DeliveryMethod.SHIPPING
                ? LocalDateTime.now().plusHours(shippingQuoteReservationTtlHours)
                : LocalDateTime.now().plusMinutes(pickupReservationTtlMinutes);
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
                        pickupReservationTtlMinutes * 60
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
            order.setShippingCost(BigDecimal.ZERO);
            order.setShippingAddress(normalizedShippingAddress);
            order.setZipCode(normalizedZipCode);
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

        log.info("Order {} created in {} state with {} reservation(s), expires at {}",
                savedOrder.getId(), savedOrder.getStatus(), consolidatedCart.size(), expiresAt);
        return savedOrder;
    }

    @Override
    @Transactional
    public Order createPendingShippingOrderAndNotify(CheckoutRequest checkoutRequest, User authenticatedUser) {
        Order order = createPendingOrder(checkoutRequest, authenticatedUser);
        if (order.getDeliveryMethod() != DeliveryMethod.SHIPPING) {
            return order;
        }

        eventPublisher.publishEvent(new ShippingQuoteRequestedEvent(this, buildShippingQuoteRequestedData(order)));
        return order;
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
    public Order prepareShippingQuote(Long orderId, BigDecimal shippingCost) {
        if (shippingCost == null || shippingCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Shipping cost must be non-negative");
        }

        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getDeliveryMethod() != DeliveryMethod.SHIPPING) {
            throw new ConflictException("Shipping quote can only be published for SHIPPING orders");
        }

        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new ConflictException("Cannot publish shipping quote for order in status: " + order.getStatus());
        }

        BigDecimal normalizedShippingCost = shippingCost.stripTrailingZeros();
        BigDecimal currentShippingCost = order.getShippingCost() == null
                ? BigDecimal.ZERO
                : order.getShippingCost().stripTrailingZeros();

        if (order.getStatus() == OrderStatus.QUOTE_READY_PAYMENT_PENDING
                && normalizedShippingCost.compareTo(currentShippingCost) == 0
                && order.getPreferenceId() != null
                && !order.getPreferenceId().isBlank()) {
            resetQuotePaymentWindow(order);
            return order;
        }

        BigDecimal subtotal = order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setShippingCost(shippingCost);
        order.setTotalAmount(subtotal.add(shippingCost));
        order.setStatus(OrderStatus.QUOTE_READY_PAYMENT_PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        if (normalizedShippingCost.compareTo(currentShippingCost) != 0) {
            order.setPreferenceId(null);
        }

        resetQuotePaymentWindow(order);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.INITIATED
                && order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.QUOTE_READY_PAYMENT_PENDING) {
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
        order.setPaymentStatus(PaymentStatus.APPROVED);
        orderRepository.save(order);
        OrderPaidNotificationData adminNotificationData = buildOrderPaidNotificationData(order);
        CustomerOrderConfirmationData customerConfirmationData = buildCustomerOrderConfirmationData(order);
        eventPublisher.publishEvent(new AdminOrderNotificationEvent(this, adminNotificationData));
        eventPublisher.publishEvent(new CustomerOrderConfirmationEvent(this, customerConfirmationData));

        log.info("Order {} confirmed as PAID. {} reservation(s) consumed.", orderId, activeReservations.size());
    }

    @Override
    @Transactional
    public void markOrderAsPending(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.QUOTE_READY_PAYMENT_PENDING) {
            log.info("Order {} is already in pending-payment flow ({}). Skipping.", orderId, order.getStatus());
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

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
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
        if (order.getPaymentStatus() != PaymentStatus.APPROVED) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        }
        orderRepository.save(order);

        log.info("Order {} cancelled. {} reservation(s) released.", order.getId(), activeReservations.size());
    }

    private void validateStatusTransition(Order order, OrderStatus newStatus) {
        transitionPolicy.validateTransition(order, newStatus);
    }

    /**
     * Resets the payment window for a quote-ready order to {@code quoteReadyTtlHours} from now.
     * Updates both the order's {@code quoteExpiresAt} and the {@code expiresAt} of all its ACTIVE
     * stock reservations, so the cleanup scheduler cancels the order if the customer does not pay
     * within that window.
     */
    private void resetQuotePaymentWindow(Order order) {
        LocalDateTime newExpiry = LocalDateTime.now().plusHours(quoteReadyTtlHours);
        order.setQuoteExpiresAt(newExpiry);
        orderRepository.save(order);

        List<StockReservation> activeReservations =
                stockReservationRepository.findByOrderIdAndStatus(order.getId(), ReservationStatus.ACTIVE);
        for (StockReservation reservation : activeReservations) {
            reservation.setExpiresAt(newExpiry);
            stockReservationRepository.save(reservation);
        }
        log.info("Quote payment window reset for order {}. Expires at {}.", order.getId(), newExpiry);
    }

    private String normalizeShippingAddress(String shippingAddress) {
        if (shippingAddress == null || shippingAddress.isBlank()) {
            throw new IllegalArgumentException("Shipping address is required for delivery orders");
        }
        String normalizedAddress = shippingAddress.trim().replaceAll("\\s+", " ");
        if (normalizedAddress.length() < SHIPPING_ADDRESS_MIN_LENGTH || normalizedAddress.length() > SHIPPING_ADDRESS_MAX_LENGTH) {
            throw new IllegalArgumentException("Shipping address must be between 5 and 200 characters");
        }
        if (!SHIPPING_ADDRESS_PATTERN.matcher(normalizedAddress).matches()) {
            throw new IllegalArgumentException("Shipping address contains invalid characters");
        }
        return normalizedAddress;
    }

    private String normalizeZipCode(String zipCode) {
        if (zipCode == null || zipCode.isBlank()) {
            throw new IllegalArgumentException("ZIP code is required for delivery orders");
        }
        String normalizedZipCode = zipCode.trim().toUpperCase(Locale.ROOT);
        if (!ZIP_CODE_PATTERN.matcher(normalizedZipCode).matches()) {
            throw new IllegalArgumentException("ZIP code must be 4 digits or CPA format (A9999AAA)");
        }
        return normalizedZipCode;
    }

    private ShippingQuoteRequestedData buildShippingQuoteRequestedData(Order order) {
        String firstName = order.getUser().getFirstName() == null ? "" : order.getUser().getFirstName().trim();
        String lastName = order.getUser().getLastName() == null ? "" : order.getUser().getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isBlank()) {
            fullName = "Cliente sin nombre";
        }

        List<ShippingQuoteRequestedData.ShippingQuoteItemData> items = order.getItems().stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new ShippingQuoteRequestedData.ShippingQuoteItemData(
                            item.getProduct().getName(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            lineTotal
                    );
                })
                .toList();

        BigDecimal productsSubtotal = items.stream()
                .map(ShippingQuoteRequestedData.ShippingQuoteItemData::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ShippingQuoteRequestedData(
                order.getId(),
                fullName,
                order.getUser().getEmail(),
                order.getContactPhone(),
                order.getShippingAddress(),
                order.getZipCode(),
                productsSubtotal,
                items
        );
    }

    private OrderPaidNotificationData buildOrderPaidNotificationData(Order order) {
        String firstName = order.getUser().getFirstName() == null ? "" : order.getUser().getFirstName().trim();
        String lastName = order.getUser().getLastName() == null ? "" : order.getUser().getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        if (fullName.isBlank()) {
            fullName = "Cliente sin nombre";
        }

        List<OrderPaidNotificationData.OrderPaidItemData> items = order.getItems().stream()
                .map(item -> new OrderPaidNotificationData.OrderPaidItemData(
                        item.getProduct().getName(),
                        item.getQuantity()))
                .toList();

        return new OrderPaidNotificationData(
                order.getId(),
                fullName,
                order.getUser().getEmail(),
                order.getContactPhone(),
                items,
                order.getTotalAmount(),
                order.getDeliveryMethod()
        );
    }

    private CustomerOrderConfirmationData buildCustomerOrderConfirmationData(Order order) {
        String firstName = order.getUser().getFirstName() == null ? "" : order.getUser().getFirstName().trim();
        String lastName = order.getUser().getLastName() == null ? "" : order.getUser().getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        if (fullName.isBlank()) {
            fullName = "Cliente";
        }

        List<String> productPreviewImages = order.getItems().stream()
                .map(OrderItem::getProduct)
                .filter(Objects::nonNull)
                .map(this::resolveProductPreviewImage)
                .filter(Objects::nonNull)
                .limit(3)
                .toList();

        return new CustomerOrderConfirmationData(
                order.getId(),
                fullName,
                order.getUser().getEmail(),
                productPreviewImages,
                order.getTotalAmount(),
                order.getDeliveryMethod(),
                order.getDeliveryMethod() == DeliveryMethod.SHIPPING ? order.getShippingAddress() : null,
                order.getDeliveryMethod() == DeliveryMethod.SHIPPING ? order.getZipCode() : null
        );
    }

    private OrderStatusUpdatedData buildOrderStatusUpdatedData(Order order) {
        String firstName = order.getUser().getFirstName() == null ? "" : order.getUser().getFirstName().trim();
        String lastName = order.getUser().getLastName() == null ? "" : order.getUser().getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isBlank()) {
            fullName = "Cliente";
        }

        List<String> productPreviewImages = order.getItems().stream()
                .map(OrderItem::getProduct)
                .filter(Objects::nonNull)
                .map(this::resolveProductPreviewImage)
                .filter(Objects::nonNull)
                .limit(3)
                .toList();

        return new OrderStatusUpdatedData(
                order.getId(),
                fullName,
                order.getUser().getEmail(),
                order.getStatus(),
                order.getDeliveryMethod(),
                order.getTotalAmount(),
                productPreviewImages
        );
    }

    private String resolveProductPreviewImage(Product product) {
        if (product.getImages() != null) {
            for (String image : product.getImages()) {
                if (image != null && !image.isBlank()) {
                    return image;
                }
            }
        }

        if (product.getCategory() != null) {
            String defaultImageUrl = product.getCategory().getDefaultImageUrl();
            if (defaultImageUrl != null && !defaultImageUrl.isBlank()) {
                return defaultImageUrl;
            }
        }

        return null;
    }
}
