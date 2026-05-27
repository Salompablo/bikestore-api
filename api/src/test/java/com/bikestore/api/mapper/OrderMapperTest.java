package com.bikestore.api.mapper;

import com.bikestore.api.dto.response.AdminOrderDetailResponse;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.entity.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    @DisplayName("toOrderResponse includes subtotal and checkout URL when preference exists")
    void toOrderResponseIncludesSubtotalAndCheckoutUrl() {
        Order order = buildOrder(OrderStatus.QUOTE_READY_PAYMENT_PENDING, "pref-123");

        OrderResponse response = mapper.toOrderResponse(order);

        assertEquals(BigDecimal.valueOf(3100), response.subtotalAmount());
        assertEquals("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=pref-123", response.checkoutUrl());
        assertTrue(response.payableNow());
        assertFalse(response.requiresShippingQuote());
    }

    @Test
    @DisplayName("toAdminOrderDetailResponse includes customer and quoting data")
    void toAdminOrderDetailResponseIncludesCustomerAndQuotingData() {
        Order order = buildOrder(OrderStatus.QUOTE_REQUESTED, null);

        AdminOrderDetailResponse response = mapper.toAdminOrderDetailResponse(order);

        assertEquals("Ada Lovelace", response.customerFullName());
        assertEquals("ada@test.com", response.customerEmail());
        assertEquals(BigDecimal.valueOf(3100), response.subtotalAmount());
        assertTrue(response.requiresShippingQuote());
        assertFalse(response.payableNow());
    }

    private Order buildOrder(OrderStatus status, String preferenceId) {
        Product firstProduct = Product.builder()
                .id(1L)
                .name("Bike One")
                .images(List.of("https://cdn.example.com/1.jpg"))
                .build();
        Product secondProduct = Product.builder()
                .id(2L)
                .name("Bike Two")
                .images(List.of())
                .build();

        OrderItem firstItem = OrderItem.builder()
                .product(firstProduct)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(1500))
                .build();
        OrderItem secondItem = OrderItem.builder()
                .product(secondProduct)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(800))
                .build();

        return Order.builder()
                .id(88L)
                .user(User.builder()
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .email("ada@test.com")
                        .build())
                .status(status)
                .deliveryMethod(DeliveryMethod.SHIPPING)
                .totalAmount(BigDecimal.valueOf(3300))
                .shippingCost(BigDecimal.valueOf(200))
                .contactPhone("+5492235551234")
                .paymentStatus(PaymentStatus.PENDING)
                .preferenceId(preferenceId)
                .shippingAddress("Calle Falsa 123")
                .zipCode("7600")
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now())
                .items(List.of(firstItem, secondItem))
                .build();
    }
}
