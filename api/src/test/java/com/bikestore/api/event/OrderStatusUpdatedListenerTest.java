package com.bikestore.api.event;

import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderStatusUpdatedListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderStatusUpdatedListener listener;

    @Test
    @DisplayName("Listener delegates READY_FOR_PICKUP notification to EmailService")
    void handleDelegatesToEmailServiceForPickup() {
        OrderStatusUpdatedData data = new OrderStatusUpdatedData(
                10L,
                "Ana Gomez",
                "ana@test.com",
                OrderStatus.READY_FOR_PICKUP,
                DeliveryMethod.STORE_PICKUP,
                BigDecimal.valueOf(1500),
                List.of("https://cdn.example.com/bike.jpg")
        );

        listener.handle(new OrderStatusUpdatedEvent(this, data));

        verify(emailService).sendOrderStatusNotification(data);
    }

    @Test
    @DisplayName("Listener delegates SHIPPED notification to EmailService")
    void handleDelegatesToEmailServiceForShipped() {
        OrderStatusUpdatedData data = new OrderStatusUpdatedData(
                20L,
                "Carlos Lopez",
                "carlos@test.com",
                OrderStatus.SHIPPED,
                DeliveryMethod.SHIPPING,
                BigDecimal.valueOf(3000),
                List.of()
        );

        listener.handle(new OrderStatusUpdatedEvent(this, data));

        verify(emailService).sendOrderStatusNotification(data);
    }

    @Test
    @DisplayName("Listener swallows email delivery exceptions without rethrowing")
    void handleSwallowsExceptions() {
        OrderStatusUpdatedData data = new OrderStatusUpdatedData(
                30L,
                "Pedro Ruiz",
                "pedro@test.com",
                OrderStatus.READY_FOR_PICKUP,
                DeliveryMethod.STORE_PICKUP,
                BigDecimal.valueOf(800),
                List.of()
        );

        doThrow(new RuntimeException("email service unavailable"))
                .when(emailService).sendOrderStatusNotification(data);

        listener.handle(new OrderStatusUpdatedEvent(this, data));

        verify(emailService).sendOrderStatusNotification(data);
    }
}
