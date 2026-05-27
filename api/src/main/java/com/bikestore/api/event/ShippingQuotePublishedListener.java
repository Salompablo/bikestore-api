package com.bikestore.api.event;

import com.bikestore.api.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingQuotePublishedListener {

    private final EmailService emailService;

    @Async("adminEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ShippingQuotePublishedEvent event) {
        try {
            emailService.sendCustomerShippingQuoteReady(event.getData());
            log.info("Customer shipping quote ready email sent for order {}", event.getData().orderId());
        } catch (Exception e) {
            log.error("Failed to send customer shipping quote ready email for order {}", event.getData().orderId(), e);
        }
    }
}
