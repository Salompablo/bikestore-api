package com.bikestore.api.event;

import com.bikestore.api.dto.data.CustomerOrderConfirmationData;
import com.bikestore.api.entity.enums.DeliveryMethod;
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
class CustomerOrderConfirmationListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CustomerOrderConfirmationListener listener;

    @Test
    @DisplayName("Listener delegates customer confirmation email to EmailService")
    void handleCustomerOrderConfirmationDelegatesToEmailService() {
        CustomerOrderConfirmationData data = new CustomerOrderConfirmationData(
                15L,
                "Ada Lovelace",
                "ada@test.com",
                List.of("https://cdn.example.com/bike.jpg"),
                BigDecimal.valueOf(1200),
                DeliveryMethod.SHIPPING,
                "Calle Falsa 123",
                "7600"
        );

        listener.handleCustomerOrderConfirmation(new CustomerOrderConfirmationEvent(this, data));

        verify(emailService).sendCustomerOrderConfirmation(data);
    }

    @Test
    @DisplayName("Listener swallows email delivery exceptions")
    void handleCustomerOrderConfirmationSwallowsExceptions() {
        CustomerOrderConfirmationData data = new CustomerOrderConfirmationData(
                22L,
                "Grace Hopper",
                "grace@test.com",
                List.of(),
                BigDecimal.valueOf(2500),
                DeliveryMethod.STORE_PICKUP,
                null,
                null
        );

        doThrow(new RuntimeException("boom")).when(emailService).sendCustomerOrderConfirmation(data);

        listener.handleCustomerOrderConfirmation(new CustomerOrderConfirmationEvent(this, data));

        verify(emailService).sendCustomerOrderConfirmation(data);
    }
}
