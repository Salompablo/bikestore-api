package com.bikestore.api.service.impl;

import com.bikestore.api.dto.data.CustomerOrderConfirmationData;
import com.bikestore.api.dto.request.CartItemRequest;
import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.entity.Category;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.StockReservation;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.entity.enums.ReservationStatus;
import com.bikestore.api.event.AdminOrderNotificationEvent;
import com.bikestore.api.event.CustomerOrderConfirmationEvent;
import com.bikestore.api.event.OrderPaidNotificationData;
import com.bikestore.api.event.OrderStatusUpdatedData;
import com.bikestore.api.event.OrderStatusUpdatedEvent;
import com.bikestore.api.mapper.OrderMapper;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.StockReservationRepository;
import com.bikestore.api.service.OrderStatusTransitionPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OrderStatusTransitionPolicy transitionPolicy;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("confirmOrder publishes admin and customer events with materialized data")
    void confirmOrderPublishesAdminAndCustomerEvents() {
        Category category = Category.builder()
                .name("MTB")
                .defaultImageUrl("https://cdn.example.com/category.jpg")
                .build();

        Product productWithImage = Product.builder()
                .id(1L)
                .name("Bike One")
                .price(BigDecimal.valueOf(1000))
                .stock(10)
                .reservedStock(1)
                .category(category)
                .images(List.of("https://cdn.example.com/product-1.jpg"))
                .build();

        Product productWithFallback = Product.builder()
                .id(2L)
                .name("Bike Two")
                .price(BigDecimal.valueOf(500))
                .stock(10)
                .reservedStock(1)
                .category(category)
                .images(List.of())
                .build();

        OrderItem firstItem = OrderItem.builder()
                .product(productWithImage)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(1000))
                .build();

        OrderItem secondItem = OrderItem.builder()
                .product(productWithFallback)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(500))
                .build();

        User user = User.builder()
                .email("customer@test.com")
                .firstName("Ada")
                .lastName("Lovelace")
                .build();

        Order order = Order.builder()
                .id(99L)
                .user(user)
                .status(OrderStatus.PENDING)
                .deliveryMethod(DeliveryMethod.STORE_PICKUP)
                .totalAmount(BigDecimal.valueOf(2000))
                .items(List.of(firstItem, secondItem))
                .build();

        StockReservation reservation = StockReservation.builder()
                .product(productWithImage)
                .quantity(1)
                .status(ReservationStatus.ACTIVE)
                .order(order)
                .build();

        when(orderRepository.findByIdWithLock(99L)).thenReturn(Optional.of(order));
        when(stockReservationRepository.findByOrderIdAndStatus(99L, ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));
        when(productRepository.deductAndReleaseStock(1L, 1)).thenReturn(1);
        when(orderRepository.save(order)).thenReturn(order);
        when(stockReservationRepository.save(reservation)).thenReturn(reservation);

        orderService.confirmOrder(99L);

        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.PAID, order.getStatus());

        List<ApplicationEvent> publishedEvents = eventCaptor.getAllValues();
        assertTrue(publishedEvents.get(0) instanceof AdminOrderNotificationEvent);
        assertTrue(publishedEvents.get(1) instanceof CustomerOrderConfirmationEvent);

        OrderPaidNotificationData adminData =
                ((AdminOrderNotificationEvent) publishedEvents.get(0)).getOrderData();
        CustomerOrderConfirmationData customerData =
                ((CustomerOrderConfirmationEvent) publishedEvents.get(1)).getOrderData();

        assertEquals(99L, adminData.orderId());
        assertEquals("Ada Lovelace", customerData.customerName());
        assertEquals("customer@test.com", customerData.customerEmail());
        assertEquals(2, customerData.productPreviewImages().size());
        assertEquals("https://cdn.example.com/product-1.jpg", customerData.productPreviewImages().get(0));
        assertEquals("https://cdn.example.com/category.jpg", customerData.productPreviewImages().get(1));
        assertEquals(DeliveryMethod.STORE_PICKUP, customerData.deliveryMethod());
        assertNull(customerData.shippingAddress());
        assertNull(customerData.zipCode());
    }

    @Test
    @DisplayName("createPendingOrder normalizes shipping address and zip code for SHIPPING")
    void createPendingOrderNormalizesShippingData() {
        User user = User.builder().id(10L).build();
        Product product = Product.builder()
                .id(1L)
                .name("Bike")
                .price(BigDecimal.valueOf(1000))
                .stock(10)
                .reservedStock(0)
                .build();

        CheckoutRequest request = new CheckoutRequest(
                List.of(new CartItemRequest(1L, 1)),
                DeliveryMethod.SHIPPING,
                "  Av. Colón   1234,   Mar del Plata  ",
                "  b7600abc  ",
                null,
                "+5492235551234",
                false
        );

        when(orderRepository.findByUserIdAndStatusIn(anyLong(), any())).thenReturn(List.of());
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));
        when(productRepository.reserveStock(1L, 1)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockReservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order created = orderService.createPendingOrder(request, user);

        assertNotNull(created);
        assertEquals("Av. Colón 1234, Mar del Plata", created.getShippingAddress());
        assertEquals("B7600ABC", created.getZipCode());
        assertEquals(OrderStatus.QUOTE_REQUESTED, created.getStatus());
    }

    @Test
    @DisplayName("createPendingOrder rejects invalid shipping zip code format")
    void createPendingOrderRejectsInvalidZipCodeFormat() {
        User user = User.builder().id(10L).build();
        CheckoutRequest request = new CheckoutRequest(
                List.of(new CartItemRequest(1L, 1)),
                DeliveryMethod.SHIPPING,
                "Av. Colón 1234",
                "76-00",
                null,
                "+5492235551234",
                false
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createPendingOrder(request, user));

        assertEquals("ZIP code must be 4 digits or CPA format (A9999AAA)", ex.getMessage());
    }

    @Test
    @DisplayName("createPendingOrder rejects invalid shipping address characters")
    void createPendingOrderRejectsInvalidAddressCharacters() {
        User user = User.builder().id(10L).build();
        CheckoutRequest request = new CheckoutRequest(
                List.of(new CartItemRequest(1L, 1)),
                DeliveryMethod.SHIPPING,
                "Av. Colón 1234 <>",
                "7600",
                null,
                "+5492235551234",
                false
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createPendingOrder(request, user));

        assertEquals("Shipping address contains invalid characters", ex.getMessage());
    }

    @Test
    @DisplayName("updateOrderStatus to READY_FOR_PICKUP publishes OrderStatusUpdatedEvent with correct data")
    void updateOrderStatusToReadyForPickupPublishesEvent() {
        Product product = Product.builder()
                .id(1L)
                .name("Bike One")
                .price(BigDecimal.valueOf(1000))
                .stock(10)
                .reservedStock(0)
                .images(List.of("https://cdn.example.com/bike.jpg"))
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(1000))
                .build();

        User user = User.builder()
                .email("customer@test.com")
                .firstName("Ana")
                .lastName("Gomez")
                .build();

        Order order = Order.builder()
                .id(42L)
                .user(user)
                .status(OrderStatus.PAID)
                .deliveryMethod(DeliveryMethod.STORE_PICKUP)
                .totalAmount(BigDecimal.valueOf(1000))
                .items(List.of(item))
                .build();

        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateOrderStatus(42L, OrderStatus.READY_FOR_PICKUP);

        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        ApplicationEvent published = captor.getValue();
        assertTrue(published instanceof OrderStatusUpdatedEvent);

        OrderStatusUpdatedData data = ((OrderStatusUpdatedEvent) published).getData();
        assertEquals(42L, data.orderId());
        assertEquals("Ana Gomez", data.customerName());
        assertEquals("customer@test.com", data.customerEmail());
        assertEquals(OrderStatus.READY_FOR_PICKUP, data.newStatus());
        assertEquals(DeliveryMethod.STORE_PICKUP, data.deliveryMethod());
        assertEquals(BigDecimal.valueOf(1000), data.totalAmount());
        assertEquals(1, data.productPreviewImages().size());
        assertEquals("https://cdn.example.com/bike.jpg", data.productPreviewImages().get(0));
    }

    @Test
    @DisplayName("updateOrderStatus to SHIPPED publishes OrderStatusUpdatedEvent with correct data")
    void updateOrderStatusToShippedPublishesEvent() {
        Product product = Product.builder()
                .id(2L)
                .name("Bike Two")
                .price(BigDecimal.valueOf(2000))
                .stock(5)
                .reservedStock(0)
                .images(List.of())
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(2000))
                .build();

        User user = User.builder()
                .email("rider@test.com")
                .firstName("Carlos")
                .lastName("Lopez")
                .build();

        Order order = Order.builder()
                .id(55L)
                .user(user)
                .status(OrderStatus.PAID)
                .deliveryMethod(DeliveryMethod.SHIPPING)
                .totalAmount(BigDecimal.valueOf(2000))
                .items(List.of(item))
                .build();

        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateOrderStatus(55L, OrderStatus.SHIPPED);

        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        ApplicationEvent published = captor.getValue();
        assertTrue(published instanceof OrderStatusUpdatedEvent);

        OrderStatusUpdatedData data = ((OrderStatusUpdatedEvent) published).getData();
        assertEquals(55L, data.orderId());
        assertEquals("Carlos Lopez", data.customerName());
        assertEquals("rider@test.com", data.customerEmail());
        assertEquals(OrderStatus.SHIPPED, data.newStatus());
        assertEquals(DeliveryMethod.SHIPPING, data.deliveryMethod());
        assertTrue(data.productPreviewImages().isEmpty());
    }

    @Test
    @DisplayName("updateOrderStatus to a non-notifiable status does not publish any event")
    void updateOrderStatusToPickedUpDoesNotPublishEvent() {
        User user = User.builder()
                .email("customer@test.com")
                .firstName("Maria")
                .lastName("Perez")
                .build();

        Order order = Order.builder()
                .id(77L)
                .user(user)
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryMethod(DeliveryMethod.STORE_PICKUP)
                .totalAmount(BigDecimal.valueOf(500))
                .items(List.of())
                .build();

        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateOrderStatus(77L, OrderStatus.PICKED_UP);

        verify(eventPublisher, never()).publishEvent(any(ApplicationEvent.class));
    }
}
