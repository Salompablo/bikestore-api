package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiCustomerErrors;
import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Endpoints for processing purchases and payments via Mercado Pago")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @Operation(summary = "Create payment preference", description = "Generates a Mercado Pago preference ID for the provided cart items. Requires CUSTOMER privileges.")
    @ApiResponse(responseCode = "200", description = "Preference created successfully",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                        "preferenceId": "123456789-abcdef-1234-5678-abcdef123456"
                    }
                    """)))
    @ApiCustomerErrors
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/create-preference")
    public ResponseEntity<Map<String, String>> createPreference(@Valid @RequestBody CheckoutRequest request) {
        String preferenceId = checkoutService.createPaymentPreference(request);
        return ResponseEntity.ok(Map.of("preferenceId", preferenceId));
    }
}