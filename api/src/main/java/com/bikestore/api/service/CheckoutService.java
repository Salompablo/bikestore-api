package com.bikestore.api.service;

import com.bikestore.api.dto.request.CartItemRequest;
import com.bikestore.api.dto.request.CartRequest;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.UserRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public String createPaymentPreference(CartRequest cartRequest) {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PreferenceItemRequest> mpItems = new ArrayList<>();

        for (CartItemRequest itemReq : cartRequest.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.productId()));

            if (product.getStock() < itemReq.quantity()) {
                throw new ConflictException("Not enough stock for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.quantity());
            orderItem.setUnitPrice(product.getPrice());
            order.addItem(orderItem);

            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(itemReq.quantity()));
            totalAmount = totalAmount.add(itemTotal);

            PreferenceItemRequest mpItem = PreferenceItemRequest.builder()
                    .id(product.getId().toString())
                    .title(product.getName())
                    .quantity(itemReq.quantity())
                    .currencyId("ARS")
                    .unitPrice(product.getPrice())
                    .build();
            mpItems.add(mpItem);
        }

        order.setTotalAmount(totalAmount);

        order = orderRepository.save(order);

        try {
            PreferenceClient client = new PreferenceClient();
            PreferenceRequest request = PreferenceRequest.builder()
                    .items(mpItems)
                    .externalReference(order.getId().toString())
                    .build();

            Preference preference = client.create(request);

            order.setPreferenceId(preference.getId());
            orderRepository.save(order);

            log.info("Order {} saved. MP Preference created: {}", order.getId(), preference.getId());
            return preference.getId();

        } catch (com.mercadopago.exceptions.MPApiException apiException) {
            log.error("Mercado Pago rejected the request. Status: {}, Details: {}",
                    apiException.getApiResponse().getStatusCode(),
                    apiException.getApiResponse().getContent());
            throw new RuntimeException("Mercado Pago API error");
        } catch (Exception e) {
            log.error("Error communicating with Mercado Pago", e);
            throw new RuntimeException("Error processing payment");
        }
    }

    @Transactional
    public void processWebHook(Long paymentId) {
        try {
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(paymentId);

            log.info("Payment details received. Status: {}", payment.getStatus());

            if ("approved".equals(payment.getStatus())) {

                String orderIdStr = payment.getExternalReference();
                if (orderIdStr == null) {
                    log.warn("Payment {} has no external reference. Skipping.", paymentId);
                    return;
                }

                Long orderId = Long.parseLong(orderIdStr);
                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

                if (order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.PAID);

                    for (OrderItem item : order.getItems()) {
                        Product product = item.getProduct();
                        int newStock = product.getStock() - item.getQuantity();
                        product.setStock(Math.max(newStock, 0));
                        productRepository.save(product);
                    }

                    orderRepository.save(order);
                    log.info("Order {} successfully marked as PAID. Inventory updated.", orderId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing Mercado Pago Webhook for payment: " + paymentId, e);
        }
    }

}