package com.bikestore.api.service.impl;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.service.CheckoutInitializationStrategy;
import com.bikestore.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShippingQuoteCheckoutStrategy implements CheckoutInitializationStrategy {

    private final OrderService orderService;

    @Override
    public DeliveryMethod supportedMethod() {
        return DeliveryMethod.SHIPPING;
    }

    @Override
    public CheckoutResponse initialize(CheckoutRequest request, User authenticatedUser) {
        Order order = orderService.createPendingShippingOrderAndNotify(request, authenticatedUser);

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
