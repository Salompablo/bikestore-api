package com.bikestore.api.controller;

import com.bikestore.api.dto.request.CartRequest;
import com.bikestore.api.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/create-preference")
    public ResponseEntity<Map<String, String>> createPreference(@Valid @RequestBody CartRequest request) {
        String preferenceId = checkoutService.createPaymentPreference(request);
        return ResponseEntity.ok(Map.of("preferenceId", preferenceId));
    }
}
