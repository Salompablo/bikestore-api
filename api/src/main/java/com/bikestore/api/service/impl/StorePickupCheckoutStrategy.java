package com.bikestore.api.service.impl;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutInfo;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.service.CheckoutInitializationStrategy;
import com.bikestore.api.service.OrderService;
import com.bikestore.api.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorePickupCheckoutStrategy implements CheckoutInitializationStrategy {

    private final PaymentGatewayService paymentGatewayService;
    private final OrderService orderService;

    @Override
    public DeliveryMethod supportedMethod() {
        return DeliveryMethod.STORE_PICKUP;
    }

    @Override
    public CheckoutResponse initialize(CheckoutRequest request, User authenticatedUser) {
        Order order = orderService.createPendingOrder(request, authenticatedUser);
        try {
            CheckoutInfo checkoutInfo = paymentGatewayService.createPreference(order);
            orderService.updateOrderPreference(order.getId(), checkoutInfo.preferenceId());
            log.info("Checkout initialized for pickup order {}. MP Preference: {}", order.getId(), checkoutInfo.preferenceId());
            return new CheckoutResponse(
                    order.getId(),
                    checkoutInfo.preferenceId(),
                    checkoutInfo.initPoint(),
                    false,
                    true,
                    "CHECKOUT_READY"
            );
        } catch (Exception e) {
            log.error("Failed to create MP preference for pickup order {}. Releasing reservation.", order.getId(), e);
            try {
                orderService.cancelOrder(order.getId(), authenticatedUser);
            } catch (Exception compensationError) {
                log.error("Compensation cancelOrder failed for pickup order {}.", order.getId(), compensationError);
            }
            throw new RuntimeException("Payment initialization failed. Please retry.", e);
        }
    }
}
