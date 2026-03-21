package com.bikestore.api.service;

import com.bikestore.api.dto.response.CheckoutInfo;
import com.bikestore.api.dto.response.PaymentInfo;
import com.bikestore.api.entity.Order;

public interface PaymentGatewayService {
    CheckoutInfo createPreference(Order order);
    PaymentInfo getPaymentInfo(Long paymentId);
}
