package com.bikestore.api.service.impl;

import com.bikestore.api.dto.data.CustomerOrderConfirmationData;
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
import com.bikestore.api.mapper.OrderMapper;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.StockReservationRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
