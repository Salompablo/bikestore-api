package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiPublicErrors;
import com.bikestore.api.security.MercadoPagoIpValidator;
import com.bikestore.api.security.WebhookRateLimiter;
import com.bikestore.api.security.WebhookSignatureValidator;
import com.bikestore.api.service.CheckoutFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final MercadoPagoIpValidator ipValidator;
    private final WebhookRateLimiter rateLimiter;

    /** Prefix used to namespace IPN-based deduplication event IDs stored in {@link com.bikestore.api.entity.WebhookEvent}. */
    private static final String IPN_EVENT_ID_PREFIX = "ipn-";

    @Operation(
            summary = "Mercado Pago Webhook & IPN Receiver",
            description = """
                    Securely receives asynchronous payment status updates from Mercado Pago.
                    - V1 Webhooks (x-signature + x-request-id present): validated via HMAC-SHA256 signature.
                    - V2 IPN (no signature headers): validated by IP allowlist + per-IP rate limiting.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Notification successfully received and processed (or safely ignored)")
    @ApiResponse(responseCode = "429", description = "IPN rate limit exceeded for origin IP")
    @ApiPublicErrors
    @PostMapping("/mercadopago")
    public ResponseEntity<String> receiveWebhook(
            @Parameter(description = "Cryptographic signature from MP (present in Webhooks, absent in IPNs)")
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
            @RequestParam(value = "type", required = false) String type,

            HttpServletRequest request) {

        try {
            String actualId = dataIdUrl != null ? dataIdUrl : ipnId;
            String webhookKind = xSignature != null || xRequestId != null ? "signed_webhook" : "ipn";

            if (actualId == null) {
                log.info("webhook_received kind={} payment_id={} topic={} type={} action={} response_status={}",
                        webhookKind, null, topic, type, "ignored_empty_payload", 200);
                return ResponseEntity.ok("Test OK");
            }

            if ("merchant_order".equals(topic)) {
                log.info("webhook_received kind={} payment_id={} topic={} type={} action={} response_status={}",
                        webhookKind, actualId, topic, type, "ignored_merchant_order", 200);
                return ResponseEntity.ok("Ignored merchant_order");
            }

            if (xSignature != null && xRequestId != null) {
                // V1 Webhook: cryptographic HMAC-SHA256 signature validation
                if (!signatureValidator.isValid(xSignature, xRequestId, actualId)) {
                    log.warn("webhook_rejected kind=signed_webhook payment_id={} topic={} type={} reason={} response_status={}",
                            actualId, topic, type, "invalid_signature", 403);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
                }
                log.info("webhook_received kind=signed_webhook payment_id={} topic={} type={} action={} response_status={}",
                        actualId, topic, type, "accepted", 200);
                if ("payment".equals(type) || "payment".equals(topic)) {
                    checkoutFacade.processWebHook(Long.valueOf(actualId), xRequestId, topic, type);
                }
            } else if (xSignature != null || xRequestId != null) {
                log.warn("webhook_rejected kind=signed_webhook payment_id={} topic={} type={} reason={} response_status={}",
                        actualId, topic, type, "missing_signature_headers", 403);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Missing signature headers");
            } else {
                // V2 IPN: no signature — validate by IP allowlist then rate limit
                String clientIp = resolveClientIp(request);

                if (!ipValidator.isAllowed(clientIp)) {
                    log.warn("webhook_rejected kind=ipn payment_id={} topic={} type={} origin_ip={} reason={} response_status={}",
                            actualId, topic, type, clientIp, "unauthorized_origin", 403);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized origin");
                }

                if (!rateLimiter.isAllowed(clientIp)) {
                    log.warn("webhook_rejected kind=ipn payment_id={} topic={} type={} origin_ip={} reason={} response_status={}",
                            actualId, topic, type, clientIp, "rate_limit_exceeded", 429);
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
                }

                log.info("webhook_received kind=ipn payment_id={} topic={} type={} origin_ip={} action={} response_status={}",
                        actualId, topic, type, clientIp, "accepted", 200);
                if ("payment".equals(topic)) {
                    // Forward a traceable synthetic event ID; idempotency is resolved in CheckoutFacade by payment status.
                    checkoutFacade.processWebHook(Long.valueOf(actualId), IPN_EVENT_ID_PREFIX + actualId, topic, type);
                }
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error handling webhook payload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    /**
     * Resolves the real client IP, taking into account a trusted reverse-proxy {@code X-Forwarded-For}
     * header. When present, the first entry (the original client address) is returned.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}