package com.bikestore.api.service.impl;

import com.bikestore.api.dto.response.ShippingQuoteResponse;
import com.bikestore.api.service.ShippingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ShippingServiceImpl implements ShippingService {

    @Override
    public ShippingQuoteResponse calculateShippingCost(String zipCode, Double totalWeight) {

        BigDecimal cost;
        int estimatedDays;
        String provider = "Logística Bikes Asaro (Simulado)";

        if (zipCode.startsWith("7600")) {
            cost = new BigDecimal("2500.00");
            estimatedDays = 1;
        } else if (zipCode.startsWith("1") || zipCode.startsWith("2") || zipCode.startsWith("B")) {
            cost = new BigDecimal("15000.00");
            estimatedDays = 3;
            provider = "Andreani (Tarifa plana simulada)";
        } else {
            cost = new BigDecimal("25000.00");
            estimatedDays = 6;
            provider = "Andreani (Tarifa plana simulada)";
        }

        return new ShippingQuoteResponse(provider, cost, estimatedDays);
    }
}
