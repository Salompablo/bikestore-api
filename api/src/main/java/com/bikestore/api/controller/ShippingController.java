package com.bikestore.api.controller;

import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.dto.response.ShippingQuoteResponse;
import com.bikestore.api.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Endpoints for calculating shipping costs and delivery times")
public class ShippingController {

    private final ShippingService shippingService;

    @Operation(summary = "Get a shipping quote", description = "Calculates the shipping cost based on the destination ZIP code and the total weight of the cart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipping quote calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ZIP code or weight provided", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/quote")
    public ResponseEntity<ShippingQuoteResponse> quoteShipping(
            @Parameter(description = "Destination postal code", example = "7600", required = true)
            @RequestParam String zipCode,

            @Parameter(description = "Total weight of items in kg", example = "15.5", required = true)
            @RequestParam Double totalWeight) {

        ShippingQuoteResponse response = shippingService.calculateShippingCost(zipCode, totalWeight);
        return ResponseEntity.ok(response);
    }
}