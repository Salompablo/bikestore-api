package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiCustomerErrors;
import com.bikestore.api.annotation.ApiNotFound;
import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.service.CheckoutFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Endpoints for processing purchases and payments via Mercado Pago")
public class CheckoutController {

    private final CheckoutFacade checkoutFacade;

    @Operation(
            summary = "Initialize checkout",
            description = "Creates an order and reserves stock. STORE_PICKUP generates Mercado Pago preference immediately; SHIPPING leaves the order pending manual quote."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkout successfully initialized",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CheckoutResponse.class), examples = @ExampleObject(value = """
                            {"orderId":15,"preferenceId":"3226905474-059535ac-abe2-4a30-97be-46cf815c92b6","initPoint":"https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=3226905474-059535ac-abe2-4a30-97be-46cf815c92b6","requiresShippingQuote":false,"payableNow":true,"flowStatus":"CHECKOUT_READY"}
                            """))),
            @ApiResponse(responseCode = "404", description = "Not Found - User or Product does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = """
                    Conflict – Not enough available stock to fulfill the order.
                    The response body includes:
                    - errorCode: "RESERVED_TEMPORARILY" — some units are held by other active orders and will be released within retryAfterSeconds.
                    - retryAfterSeconds: suggested wait time in seconds before retrying.""",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @ApiCustomerErrors
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/create-preference")
    public ResponseEntity<CheckoutResponse> createPreference(
            @Valid @RequestBody CheckoutRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User authenticatedUser) {
        CheckoutResponse response = checkoutFacade.initializeCheckout(request, authenticatedUser);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Confirm payment after redirect",
            description = """
                    Server-to-server payment confirmation called by the frontend after Mercado Pago
                    redirects the user to the success back_url. Fetches the payment status directly
                    from the Mercado Pago API using the provided collection_id (payment ID) and,
                    if approved, marks the order as PAID.

                    This endpoint is the primary confirmation path for test-mode purchases, where
                    Mercado Pago Checkout Pro does not send webhook notifications.
                    In production, the webhook handles confirmation first; calling this endpoint
                    afterward is idempotent and safe.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment confirmed — returns the updated order",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request — payment does not reference a valid order",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not Found — order does not exist or does not belong to the authenticated user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @ApiCustomerErrors
    @ApiNotFound
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/confirm")
    public ResponseEntity<OrderResponse> confirmPayment(
            @Parameter(description = "Mercado Pago payment ID sent as collection_id in the back_url redirect", required = true, example = "173636870230")
            @RequestParam("collection_id") Long collectionId,

            @Parameter(description = "Order ID sent as external_reference in the back_url redirect", required = true, example = "15")
            @RequestParam("external_reference") Long externalReference,

            @Parameter(hidden = true) @AuthenticationPrincipal User authenticatedUser) {

        OrderResponse response = checkoutFacade.confirmPaymentByCollectionId(collectionId, externalReference, authenticatedUser);
        return ResponseEntity.ok(response);
    }
}