package com.bikestore.api.service.impl;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.event.ShippingQuoteRequestedData;
import com.bikestore.api.event.ShippingQuoteRequestedEvent;
import com.bikestore.api.service.CheckoutInitializationStrategy;
import com.bikestore.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShippingQuoteCheckoutStrategy implements CheckoutInitializationStrategy {

    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public DeliveryMethod supportedMethod() {
        return DeliveryMethod.SHIPPING;
    }

    @Override
    public CheckoutResponse initialize(CheckoutRequest request, User authenticatedUser) {
        Order order = orderService.createPendingOrder(request, authenticatedUser);

        String firstName = authenticatedUser.getFirstName() == null ? "" : authenticatedUser.getFirstName().trim();
        String lastName = authenticatedUser.getLastName() == null ? "" : authenticatedUser.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isBlank()) {
            fullName = "Cliente sin nombre";
        }

        eventPublisher.publishEvent(new ShippingQuoteRequestedEvent(this, new ShippingQuoteRequestedData(
                order.getId(),
                fullName,
                authenticatedUser.getEmail(),
                order.getContactPhone(),
                order.getShippingAddress(),
                order.getZipCode()
        )));

        return new CheckoutResponse(
                order.getId(),
                null,
                null,
                true,
                false,
                "SHIPPING_QUOTE_REQUESTED"
        );
    }
}
