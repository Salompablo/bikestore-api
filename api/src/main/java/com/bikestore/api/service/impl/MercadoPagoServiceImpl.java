package com.bikestore.api.service.impl;

import com.bikestore.api.dto.response.CheckoutInfo;
import com.bikestore.api.dto.response.PaymentInfo;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.service.PaymentGatewayService;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MercadoPagoServiceImpl implements PaymentGatewayService {

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    @Override
    public CheckoutInfo createPreference(Order order) {
        try {
            List<PreferenceItemRequest> mpItems = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                mpItems.add(PreferenceItemRequest.builder()
                        .id(item.getProduct().getId().toString())
                        .title(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .currencyId("ARS")
                        .unitPrice(item.getUnitPrice())
                        .build());
            }

            if (order.getShippingCost() != null && order.getShippingCost().doubleValue() > 0) {
                mpItems.add(PreferenceItemRequest.builder()
                        .id("SHIPPING")
                        .title("Costo de Envío")
                        .quantity(1)
                        .currencyId("ARS")
                        .unitPrice(order.getShippingCost())
                        .build());
            }

            PreferenceClient client = new PreferenceClient();
            PreferenceRequest request = PreferenceRequest.builder()
                    .items(mpItems)
                    .externalReference(order.getId().toString())
                    .notificationUrl(notificationUrl)
                    .build();

            Preference preference = client.create(request);
            return new CheckoutInfo(preference.getId(), preference.getInitPoint());

        } catch (Exception e) {
            log.error("Error communicating with Mercado Pago", e);
            throw new RuntimeException("Error processing payment");
        }
    }

    @Override
    public PaymentInfo getPaymentInfo(Long paymentId) {
        try {
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(paymentId);

            return new PaymentInfo(
                    payment.getStatus(),
                    payment.getExternalReference()
            );
        } catch (Exception e) {
            log.error("Error retrieving MP payment: " + paymentId, e);
            throw new RuntimeException("Mercado Pago API error");
        }
    }
}
