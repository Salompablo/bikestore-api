package com.bikestore.api.service;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutInfo;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.dto.response.PaymentInfo;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.WebhookEvent;
import com.bikestore.api.entity.enums.WebhookEventStatus;
import com.bikestore.api.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutFacade {

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final OrderService orderService;

    public CheckoutResponse initializeCheckout(CheckoutRequest request, User authenticatedUser) {
        Order order = orderService.createPendingOrder(request, authenticatedUser);
        try {
            CheckoutInfo checkoutInfo = paymentGatewayService.createPreference(order);
            orderService.updateOrderPreference(order.getId(), checkoutInfo.preferenceId());
            log.info("Checkout initialized for Order {}. MP Preference: {}", order.getId(), checkoutInfo.preferenceId());
            return new CheckoutResponse(order.getId(), checkoutInfo.preferenceId(), checkoutInfo.initPoint());
        } catch (Exception e) {
            log.error("Failed to create MP preference for Order {}. Releasing reservation.", order.getId(), e);
            try {
                orderService.cancelOrder(order.getId(), authenticatedUser);
            } catch (Exception compensationError) {
                log.error("Compensation cancelOrder also failed for Order {}. Manual intervention needed.",
                        order.getId(), compensationError);
            }
            throw new RuntimeException("Payment initialization failed. Please retry.", e);
        }
    }

    public void processWebHook(Long paymentId, String eventId) {
        if (webhookEventRepository.existsByEventId(eventId)) {
            log.info("Duplicate webhook event '{}' for payment {}. Skipping.", eventId, paymentId);
            return;
        }

        WebhookEvent event = webhookEventRepository.save(WebhookEvent.builder()
                .eventId(eventId)
                .status(WebhookEventStatus.RECEIVED)
                .payload(paymentId.toString())
                .build());

        try {
            PaymentInfo paymentInfo = paymentGatewayService.getPaymentInfo(paymentId);
            log.info("Payment details received for ID {}. Status: {}", paymentId, paymentInfo.status());

            if ("approved".equals(paymentInfo.status())) {
                String orderIdStr = paymentInfo.externalReference();

                if (orderIdStr == null || orderIdStr.isEmpty()) {
                    log.warn("Payment {} approved but has no external reference (Order ID). Skipping.", paymentId);
                    event.setStatus(WebhookEventStatus.FAILED);
                    webhookEventRepository.save(event);
                    return;
                }

                Long orderId = Long.parseLong(orderIdStr);
                orderService.confirmOrder(orderId);

            } else if ("pending".equals(paymentInfo.status())) {
                String orderIdStr = paymentInfo.externalReference();

                if (orderIdStr == null || orderIdStr.isEmpty()) {
                    log.warn("Payment {} pending but has no external reference (Order ID). Skipping.", paymentId);
                    event.setStatus(WebhookEventStatus.FAILED);
                    webhookEventRepository.save(event);
                    return;
                }

                Long orderId = Long.parseLong(orderIdStr);
                orderService.markOrderAsPending(orderId);
            }

            event.setStatus(WebhookEventStatus.PROCESSED);
            webhookEventRepository.save(event);

        } catch (Exception e) {
            log.error("Error processing Mercado Pago Webhook for payment: " + paymentId, e);
            event.setStatus(WebhookEventStatus.FAILED);
            webhookEventRepository.save(event);
        }
    }

}