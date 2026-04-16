package com.bikestore.api.service;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutInfo;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.dto.response.PaymentInfo;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutFacade {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final OrderService orderService;

    public CheckoutResponse initializeCheckout(CheckoutRequest request, User authenticatedUser) {
        Order order = orderService.createPendingOrder(request, authenticatedUser);
        CheckoutInfo checkoutInfo = paymentGatewayService.createPreference(order);
        orderService.updateOrderPreference(order.getId(), checkoutInfo.preferenceId());

        log.info("Checkout initialized for Order {}. MP Preference: {}", order.getId(), checkoutInfo.preferenceId());
        return new CheckoutResponse(order.getId(), checkoutInfo.preferenceId(), checkoutInfo.initPoint());
    }

    public void processWebHook(Long paymentId) {
        try {
            PaymentInfo paymentInfo = paymentGatewayService.getPaymentInfo(paymentId);
            log.info("Payment details received for ID {}. Status: {}", paymentId, paymentInfo.status());

            if ("approved".equals(paymentInfo.status())) {
                String orderIdStr = paymentInfo.externalReference();

                if (orderIdStr == null || orderIdStr.isEmpty()) {
                    log.warn("Payment {} approved but has no external reference (Order ID). Skipping.", paymentId);
                    return;
                }

                Long orderId = Long.parseLong(orderIdStr);
                orderService.confirmOrder(orderId);
            }

        } catch (Exception e) {
            log.error("Error processing Mercado Pago Webhook for payment: " + paymentId, e);
        }
    }

}