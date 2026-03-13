package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiPublicErrors;
import com.bikestore.api.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Endpoints for receiving asynchronous events from external providers")
public class WebhookController {

    private final CheckoutService checkoutService;

    @Operation(summary = "Mercado Pago Webhook", description = "Receives payment status updates from Mercado Pago.")
    @ApiResponse(responseCode = "200", description = "Webhook successfully processed")
    @ApiPublicErrors
    @PostMapping("/mercadopago")
    public ResponseEntity<String> receiveWebhook(
            @Parameter(description = "Payload sent by Mercado Pago") @RequestBody Map<String, Object> payload) {
        try {
            log.info("Webhook received from Mercado Pago: {}", payload);

            if (payload.containsKey("type") && "payment".equals(payload.get("type")) ||
                    payload.containsKey("action") && "payment.created".equals(payload.get("action"))) {

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");

                if (data != null && data.containsKey("id")) {
                    Long paymentId = Long.valueOf(data.get("id").toString());
                    checkoutService.processWebHook(paymentId);
                }
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error handling webhook payload", e);
            return ResponseEntity.ok("Error handled");
        }
    }
}