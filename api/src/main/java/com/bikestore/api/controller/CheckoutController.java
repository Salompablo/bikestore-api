package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiCustomerErrors;
import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.service.CheckoutFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Endpoints for processing purchases and payments via Mercado Pago")
public class CheckoutController {

    private final CheckoutFacade checkoutFacade;

    @Operation(
            summary = "Initialize checkout",
            description = "Creates a PENDING order, securely reserves product stock, and generates a Mercado Pago payment preference. Requires CUSTOMER privileges."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkout successfully initialized",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CheckoutResponse.class), examples = @ExampleObject(value = """
                            {
                                "orderId": 15,
                                "preferenceId": "3226905474-059535ac-abe2-4a30-97be-46cf815c92b6",
                                "initPoint": "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=3226905474-059535ac-abe2-4a30-97be-46cf815c92b6"
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "Not Found - User or Product does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Not enough stock remaining to fulfill the order",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @ApiCustomerErrors
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/create-preference")
    public ResponseEntity<CheckoutResponse> createPreference(@Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = checkoutFacade.initializeCheckout(request);
        return ResponseEntity.ok(response);
    }
}