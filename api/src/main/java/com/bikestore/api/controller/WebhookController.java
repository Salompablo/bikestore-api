package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiPublicErrors;
import com.bikestore.api.security.WebhookSignatureValidator;
import com.bikestore.api.service.CheckoutFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Endpoints for receiving asynchronous events from external providers")
public class WebhookController {

    private final CheckoutFacade checkoutFacade;
    private final WebhookSignatureValidator signatureValidator;

    @Operation(
            summary = "Mercado Pago Webhook & IPN Receiver",
            description = "Securely receives asynchronous payment status updates from Mercado Pago. Supports both V1 Webhooks (with cryptographic signatures) and V2 IPN Feeds."
    )
    @ApiResponse(responseCode = "200", description = "Notification successfully received and processed (or safely ignored)")
    @ApiPublicErrors
    @PostMapping("/mercadopago")
    public ResponseEntity<String> receiveWebhook(
            @Parameter(description = "Cryptographic signature from MP (Present in Webhooks, missing in IPNs)")
            @RequestHeader(value = "x-signature", required = false) String xSignature,

            @Parameter(description = "Unique request ID for signature validation")
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,

            @Parameter(description = "Payment ID sent via V1 Webhook URL")
            @RequestParam(value = "data.id", required = false) String dataIdUrl,

            @Parameter(description = "Payment ID sent via V2 IPN URL")
            @RequestParam(value = "id", required = false) String ipnId,

            @Parameter(description = "Topic of the IPN (e.g., 'payment' or 'merchant_order')")
            @RequestParam(value = "topic", required = false) String topic,

            @Parameter(description = "Type of the Webhook (e.g., 'payment')")
            @RequestParam(value = "type", required = false) String type) {

        try {
            String actualId = dataIdUrl != null ? dataIdUrl : ipnId;

            if (actualId == null) {
                log.info("Received empty test payload or malformed request from MP. Returning 200 OK.");
                return ResponseEntity.ok("Test OK");
            }

            if ("merchant_order".equals(topic)) {
                return ResponseEntity.ok("Ignored merchant_order");
            }

            if (xSignature != null && xRequestId != null) {
                if (!signatureValidator.isValid(xSignature, xRequestId, actualId)) {
                    log.warn("MP Signature mismatch for ID: {}. Rejecting webhook.", actualId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
                }
            } else {
                log.warn("Webhook missing security headers for ID: {}. Rejecting.", actualId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Missing security headers");
            }

            log.info("Processing Payment ID: {}", actualId);

            if ("payment".equals(type) || "payment".equals(topic)) {
                Long paymentId = Long.valueOf(actualId);
                checkoutFacade.processWebHook(paymentId);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error handling webhook payload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }
}