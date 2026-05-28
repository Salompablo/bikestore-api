package com.bikestore.api.service;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutInfo;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.dto.response.PaymentInfo;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.WebhookEvent;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.WebhookEventStatus;
import com.bikestore.api.event.ShippingQuotePublishedData;
import com.bikestore.api.event.ShippingQuotePublishedEvent;
import com.bikestore.api.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutFacade {

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final OrderService orderService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final List<CheckoutInitializationStrategy> checkoutStrategies;

    public CheckoutResponse initializeCheckout(CheckoutRequest request, User authenticatedUser) {
        if (request.savePhoneToProfile()) {
            userService.updateDefaultPhone(authenticatedUser, request.contactPhone());
        }

        Map<DeliveryMethod, CheckoutInitializationStrategy> strategiesByMethod = new EnumMap<>(DeliveryMethod.class);
        for (CheckoutInitializationStrategy strategy : checkoutStrategies) {
            strategiesByMethod.put(strategy.supportedMethod(), strategy);
        }

        CheckoutInitializationStrategy selected = strategiesByMethod.get(request.deliveryMethod());
        if (selected == null) {
            throw new IllegalArgumentException("Unsupported delivery method: " + request.deliveryMethod());
        }
        return selected.initialize(request, authenticatedUser);
    }

    @Transactional
    public CheckoutResponse publishShippingQuote(Long orderId, BigDecimal shippingCost) {
        Order order = orderService.prepareShippingQuote(orderId, shippingCost);

        if (order.getPreferenceId() != null && !order.getPreferenceId().isBlank()) {
            return new CheckoutResponse(order.getId(), order.getPreferenceId(), null, false, true, "CHECKOUT_READY");
        }

        CheckoutInfo checkoutInfo = paymentGatewayService.createPreference(order);
        orderService.updateOrderPreference(order.getId(), checkoutInfo.preferenceId());

        String firstName = order.getUser().getFirstName() == null ? "" : order.getUser().getFirstName().trim();
        String lastName = order.getUser().getLastName() == null ? "" : order.getUser().getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isBlank()) {
            fullName = "Cliente";
        }

        eventPublisher.publishEvent(new ShippingQuotePublishedEvent(this, new ShippingQuotePublishedData(
                order.getId(),
                fullName,
                order.getUser().getEmail(),
                calculateProductsSubtotal(order.getItems()),
                buildShippingQuoteItems(order.getItems()),
                order.getTotalAmount(),
                order.getShippingCost()
        )));

        return new CheckoutResponse(order.getId(), checkoutInfo.preferenceId(), checkoutInfo.initPoint(), false, true, "CHECKOUT_READY");
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

        private List<ShippingQuotePublishedData.ShippingQuoteItemData> buildShippingQuoteItems(List<OrderItem> orderItems) {
            return orderItems.stream()
                    .map(item -> {
                        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                        return new ShippingQuotePublishedData.ShippingQuoteItemData(
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                lineTotal
                        );
                    })
                    .toList();
        }

        private BigDecimal calculateProductsSubtotal(List<OrderItem> orderItems) {
            return orderItems.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

}
