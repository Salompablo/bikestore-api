package com.bikestore.api.event;

import com.bikestore.api.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class ShippingQuoteRequestedListener {

    private final EmailService emailService;
    private final String adminEmail;

    public ShippingQuoteRequestedListener(
            EmailService emailService,
            @Value("${app.admin.email}") String adminEmail
    ) {
        this.emailService = emailService;
        this.adminEmail = adminEmail;
    }

    @Async("adminEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ShippingQuoteRequestedEvent event) {
        try {
            emailService.sendAdminShippingQuoteRequest(adminEmail, event.getData());
            log.info("Admin shipping quote request sent for order {}", event.getData().orderId());
        } catch (Exception e) {
            log.error("Failed to send admin shipping quote request for order {}", event.getData().orderId(), e);
        }
    }
}
