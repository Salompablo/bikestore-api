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
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final OrderRepository orderRepository;
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
        processWebHook(paymentId, eventId, null, null);
    }

    public void processWebHook(Long paymentId, String eventId, String topic, String type) {
        WebhookEvent event = null;
        String responseAction = "noop";
        String rejectionReason = null;
        String mpStatus = null;
        String externalReference = null;
        String orderStatusBefore = null;
        String transitionApplied = "none";
        Long orderId = null;

        try {
            PaymentInfo paymentInfo = paymentGatewayService.getPaymentInfo(paymentId);
            mpStatus = normalize(paymentInfo.status());
            externalReference = normalize(paymentInfo.externalReference());
            String processingEventId = buildProcessingEventId(paymentId, mpStatus);

            if (webhookEventRepository.existsByEventId(processingEventId)) {
                responseAction = "duplicate_ignored";
                log.info(
                        "webhook_payment_processed payment_id={} topic={} type={} event_id={} mp_status={} external_reference={} action={} response_status={}",
                        paymentId, topic, type, eventId, mpStatus, externalReference, responseAction, 200
                );
                return;
            }

            try {
                event = webhookEventRepository.save(WebhookEvent.builder()
                        .eventId(processingEventId)
                        .status(WebhookEventStatus.RECEIVED)
                        .payload(String.format(
                                "{\"paymentId\":%d,\"incomingEventId\":\"%s\",\"topic\":\"%s\",\"type\":\"%s\",\"mpStatus\":\"%s\",\"externalReference\":\"%s\"}",
                                paymentId, safeForPayload(eventId), safeForPayload(topic), safeForPayload(type), safeForPayload(mpStatus), safeForPayload(externalReference)
                        ))
                        .build());
            } catch (DataIntegrityViolationException duplicateEventException) {
                responseAction = "duplicate_ignored";
                log.info(
                        "webhook_payment_processed payment_id={} topic={} type={} event_id={} mp_status={} external_reference={} action={} response_status={}",
                        paymentId, topic, type, eventId, mpStatus, externalReference, responseAction, 200
                );
                return;
            }

            if (externalReference == null) {
                rejectionReason = "missing_external_reference";
                responseAction = "failed";
                log.warn(
                        "webhook_payment_processed payment_id={} topic={} type={} event_id={} mp_status={} external_reference={} action={} rejection_reason={} response_status={}",
                        paymentId, topic, type, eventId, mpStatus, externalReference, responseAction, rejectionReason, 200
                );
                event.setStatus(WebhookEventStatus.FAILED);
                webhookEventRepository.save(event);
                return;
            }

            try {
                orderId = Long.parseLong(externalReference);
            } catch (NumberFormatException e) {
                rejectionReason = "invalid_external_reference";
                responseAction = "failed";
                log.warn(
                        "webhook_payment_processed payment_id={} topic={} type={} event_id={} mp_status={} external_reference={} action={} rejection_reason={} response_status={}",
                        paymentId, topic, type, eventId, mpStatus, externalReference, responseAction, rejectionReason, 200
                );
                event.setStatus(WebhookEventStatus.FAILED);
                webhookEventRepository.save(event);
                return;
            }

            Order order = orderRepository.findById(orderId).orElse(null);
            orderStatusBefore = order == null || order.getStatus() == null ? null : order.getStatus().name();

            if ("approved".equals(mpStatus)) {
                orderService.confirmOrder(orderId);
                transitionApplied = "to_paid";
            } else if ("pending".equals(mpStatus)) {
                orderService.markOrderAsPending(orderId);
                transitionApplied = "to_pending";
            } else {
                transitionApplied = "unsupported_mp_status";
            }

            event.setStatus(WebhookEventStatus.PROCESSED);
            webhookEventRepository.save(event);
            responseAction = "processed";
            log.info(
                    "webhook_payment_processed payment_id={} topic={} type={} event_id={} mp_status={} external_reference={} order_id={} order_found={} order_status_before={} transition_applied={} action={} response_status={}",
                    paymentId, topic, type, eventId, mpStatus, externalReference, orderId, order != null, orderStatusBefore, transitionApplied, responseAction, 200
            );

        } catch (Exception e) {
            log.error(
                    "webhook_payment_processed payment_id={} topic={} type={} event_id={} mp_status={} external_reference={} order_id={} order_status_before={} transition_applied={} action={} rejection_reason={} response_status={}",
                    paymentId, topic, type, eventId, mpStatus, externalReference, orderId, orderStatusBefore, transitionApplied, "failed", "exception", 500, e
            );
            if (event != null) {
                event.setStatus(WebhookEventStatus.FAILED);
                webhookEventRepository.save(event);
            }
        }
    }

    private String buildProcessingEventId(Long paymentId, String status) {
        return "payment-" + paymentId + "-status-" + (status == null ? "unknown" : status);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase();
    }

    private String safeForPayload(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"");
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
