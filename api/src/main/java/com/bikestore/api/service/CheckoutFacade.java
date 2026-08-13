package com.bikestore.api.service;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutInfo;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.dto.response.MerchantOrderInfo;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.dto.response.PaymentInfo;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.WebhookEvent;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.WebhookEventStatus;
import com.bikestore.api.event.ShippingQuotePublishedData;
import com.bikestore.api.event.ShippingQuotePublishedEvent;
import com.bikestore.api.exception.ResourceNotFoundException;
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

    public void processWebHook(Long resourceId, String eventId) {
        processWebHook(resourceId, eventId, null, null);
    }

    /**
     * Confirms a payment using the Mercado Pago {@code collection_id} (payment ID) sent as a
     * query parameter to the back_url on successful redirect.
     *
     * <p>This is the primary confirmation path for test-mode purchases, where Mercado Pago does not
     * send webhook notifications for Checkout Pro test payments. It fetches payment details
     * directly from the MP API (server-to-server), validates that the payment belongs to the
     * authenticated user's order, and — if approved — marks the order as PAID.
     *
     * @param collectionId the Mercado Pago payment ID received in the redirect URL
     * @param orderId      the order ID received in the redirect URL (from {@code external_reference})
     * @param authenticatedUser the currently authenticated customer
     * @return the updated {@link OrderResponse} for the confirmed order
     */
    @Transactional
    public OrderResponse confirmPaymentByCollectionId(Long collectionId, Long orderId, User authenticatedUser) {
        PaymentInfo paymentInfo = paymentGatewayService.getPaymentInfo(collectionId);

        String externalRef = normalize(paymentInfo.externalReference());
        Long resolvedOrderId;
        try {
            resolvedOrderId = Long.parseLong(externalRef != null ? externalRef : "");
        } catch (NumberFormatException e) {
            log.warn("checkout_confirm_rejected collection_id={} order_id={} reason=invalid_external_reference mp_ref={}",
                    collectionId, orderId, externalRef);
            throw new IllegalArgumentException("Payment does not reference a valid order.");
        }

        if (!resolvedOrderId.equals(orderId)) {
            log.warn("checkout_confirm_rejected collection_id={} order_id={} reason=external_reference_mismatch mp_ref={}",
                    collectionId, orderId, externalRef);
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(authenticatedUser.getId())) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        String mpStatus = normalize(paymentInfo.status());
        log.info("checkout_confirm collection_id={} order_id={} mp_status={}", collectionId, orderId, mpStatus);

        if ("approved".equals(mpStatus)) {
            orderService.confirmOrder(orderId);
        } else if ("pending".equals(mpStatus)) {
            orderService.markOrderAsPending(orderId);
        }

        return orderService.getMyOrderById(orderId, authenticatedUser);
    }


    public void processWebHook(Long resourceId, String eventId, String topic, String type) {
        WebhookEvent event = null;
        String responseAction = "noop";
        String rejectionReason = null;
        String mpStatus = null;
        String externalReference = null;
        String orderStatusBefore = null;
        String transitionApplied = "none";
        Long orderId = null;
        String resourceKind = isMerchantOrderEvent(topic, type) ? "merchant_order" : "payment";
        MerchantOrderInfo merchantOrderInfo = null;

        try {
            if ("merchant_order".equals(resourceKind)) {
                merchantOrderInfo = paymentGatewayService.getMerchantOrderInfo(resourceId);
                mpStatus = normalize(firstNonBlank(merchantOrderInfo.orderStatus(), merchantOrderInfo.status()));
                externalReference = normalize(merchantOrderInfo.externalReference());
            } else {
                PaymentInfo paymentInfo = paymentGatewayService.getPaymentInfo(resourceId);
                mpStatus = normalize(paymentInfo.status());
                externalReference = normalize(paymentInfo.externalReference());
            }
            String processingEventId = buildProcessingEventId(resourceKind, resourceId, mpStatus);

            if (webhookEventRepository.existsByEventId(processingEventId)) {
                responseAction = "duplicate_ignored";
                log.info(
                        "webhook_resource_processed resource_kind={} resource_id={} topic={} type={} event_id={} mp_status={} external_reference={} action={} response_status={}",
                        resourceKind, resourceId, topic, type, eventId, mpStatus, externalReference, responseAction, 200
                );
                return;
            }

            try {
                event = webhookEventRepository.save(WebhookEvent.builder()
                        .eventId(processingEventId)
                        .status(WebhookEventStatus.RECEIVED)
                        .payload(String.format(
                                "{\"resourceType\":\"%s\",\"resourceId\":%d,\"incomingEventId\":\"%s\",\"topic\":\"%s\",\"type\":\"%s\",\"mpStatus\":\"%s\",\"externalReference\":\"%s\"}",
                                safeForPayload(resourceKind), resourceId, safeForPayload(eventId), safeForPayload(topic), safeForPayload(type), safeForPayload(mpStatus), safeForPayload(externalReference)
                        ))
                        .build());
            } catch (DataIntegrityViolationException duplicateEventException) {
                responseAction = "duplicate_ignored";
                log.info(
                        "webhook_resource_processed resource_kind={} resource_id={} topic={} type={} event_id={} mp_status={} external_reference={} action={} response_status={}",
                        resourceKind, resourceId, topic, type, eventId, mpStatus, externalReference, responseAction, 200
                );
                return;
            }

            if (externalReference == null) {
                rejectionReason = "missing_external_reference";
                responseAction = "failed";
                log.warn(
                        "webhook_resource_processed resource_kind={} resource_id={} topic={} type={} event_id={} mp_status={} external_reference={} action={} rejection_reason={} response_status={}",
                        resourceKind, resourceId, topic, type, eventId, mpStatus, externalReference, responseAction, rejectionReason, 200
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
                        "webhook_resource_processed resource_kind={} resource_id={} topic={} type={} event_id={} mp_status={} external_reference={} action={} rejection_reason={} response_status={}",
                        resourceKind, resourceId, topic, type, eventId, mpStatus, externalReference, responseAction, rejectionReason, 200
                );
                event.setStatus(WebhookEventStatus.FAILED);
                webhookEventRepository.save(event);
                return;
            }

            Order order = orderRepository.findById(orderId).orElse(null);
            orderStatusBefore = order == null || order.getStatus() == null ? null : order.getStatus().name();

            if ("merchant_order".equals(resourceKind)) {
                if (merchantOrderInfo != null && merchantOrderInfo.isPaid()) {
                    orderService.confirmOrder(orderId);
                    transitionApplied = "to_paid";
                } else {
                    transitionApplied = "merchant_order_not_paid";
                }
            } else if ("approved".equals(mpStatus)) {
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
                    "webhook_resource_processed resource_kind={} resource_id={} topic={} type={} event_id={} mp_status={} external_reference={} order_id={} order_found={} order_status_before={} transition_applied={} action={} response_status={}",
                    resourceKind, resourceId, topic, type, eventId, mpStatus, externalReference, orderId, order != null, orderStatusBefore, transitionApplied, responseAction, 200
            );

        } catch (Exception e) {
            log.error(
                    "webhook_resource_processed resource_kind={} resource_id={} topic={} type={} event_id={} mp_status={} external_reference={} order_id={} order_status_before={} transition_applied={} action={} rejection_reason={} response_status={}",
                    resourceKind, resourceId, topic, type, eventId, mpStatus, externalReference, orderId, orderStatusBefore, transitionApplied, "failed", "exception", 500, e
            );
            if (event != null) {
                event.setStatus(WebhookEventStatus.FAILED);
                webhookEventRepository.save(event);
            }
        }
    }

    private String buildProcessingEventId(String resourceKind, Long resourceId, String status) {
        return resourceKind + "-" + resourceId + "-status-" + (status == null ? "unknown" : status);
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

    private boolean isMerchantOrderEvent(String topic, String type) {
        return "merchant_order".equals(normalize(topic)) || "topic_merchant_order_wh".equals(normalize(type));
    }

    private String firstNonBlank(String primary, String secondary) {
        String normalizedPrimary = normalize(primary);
        if (normalizedPrimary != null) {
            return normalizedPrimary;
        }
        return normalize(secondary);
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
