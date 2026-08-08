package com.bikestore.api.controller;

import com.bikestore.api.security.MercadoPagoIpValidator;
import com.bikestore.api.security.WebhookRateLimiter;
import com.bikestore.api.security.WebhookSignatureValidator;
import com.bikestore.api.service.CheckoutFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private CheckoutFacade checkoutFacade;

    @Mock
    private WebhookSignatureValidator signatureValidator;

    @Mock
    private MercadoPagoIpValidator ipValidator;

    @Mock
    private WebhookRateLimiter rateLimiter;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private WebhookController webhookController;

    @Test
    @DisplayName("Should forward signed merchant_order webhook after signature validation")
    void receiveWebhook_signedMerchantOrder_forwardsToCheckoutFacade() {
        when(signatureValidator.isValid("sig", "req-123", "987654")).thenReturn(true);

        ResponseEntity<String> response = webhookController.receiveWebhook(
                "sig",
                "req-123",
                "987654",
                null,
                "merchant_order",
                null,
                request
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
        verify(checkoutFacade).processWebHook(987654L, "req-123", "merchant_order", null);
    }

    @Test
    @DisplayName("Should forward merchant_order IPN identified by topic_merchant_order_wh type")
    void receiveWebhook_ipnMerchantOrderType_forwardsToCheckoutFacade() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("200.1.1.1");
        when(ipValidator.isAllowed("200.1.1.1")).thenReturn(true);
        when(rateLimiter.isAllowed("200.1.1.1")).thenReturn(true);

        ResponseEntity<String> response = webhookController.receiveWebhook(
                null,
                null,
                null,
                "456789",
                null,
                "topic_merchant_order_wh",
                request
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
        verify(checkoutFacade).processWebHook(456789L, "ipn-456789", null, "topic_merchant_order_wh");
    }
}
