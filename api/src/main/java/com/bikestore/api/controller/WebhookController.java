package com.bikestore.api.controller;

import com.bikestore.api.service.CheckoutService;
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
public class WebhookController {

    private final CheckoutService checkoutService;

    @PostMapping("/mercadopago")
    public ResponseEntity<String> receiveWebhook(@RequestBody Map<String, Object> payload) {
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
