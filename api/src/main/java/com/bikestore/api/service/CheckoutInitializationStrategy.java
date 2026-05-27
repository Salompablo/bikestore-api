package com.bikestore.api.service;

import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.DeliveryMethod;

public interface CheckoutInitializationStrategy {
    DeliveryMethod supportedMethod();
    CheckoutResponse initialize(CheckoutRequest request, User authenticatedUser);
}
