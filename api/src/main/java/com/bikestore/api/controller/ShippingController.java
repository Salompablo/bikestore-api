package com.bikestore.api.controller;

import com.bikestore.api.dto.response.ShippingQuoteResponse;
import com.bikestore.api.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;
    
    @GetMapping("/quote")
    public ResponseEntity<ShippingQuoteResponse> quoteShipping(
            @RequestParam String zipCode,
            @RequestParam Double totalWeight) {

        ShippingQuoteResponse response = shippingService.calculateShippingCost(zipCode, totalWeight);
        return ResponseEntity.ok(response);
    }
}
