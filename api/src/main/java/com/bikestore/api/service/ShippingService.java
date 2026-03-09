package com.bikestore.api.service;

import com.bikestore.api.dto.response.ShippingQuoteResponse;

public interface ShippingService {
    ShippingQuoteResponse calculateShippingCost(String zipCode, Double totalWeight);
}
