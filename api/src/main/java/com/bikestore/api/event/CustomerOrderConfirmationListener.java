package com.bikestore.api.event;

import com.bikestore.api.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class CustomerOrderConfirmationListener {

    private final EmailService emailService;

    public CustomerOrderConfirmationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("adminEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCustomerOrderConfirmation(CustomerOrderConfirmationEvent event) {
        try {
            emailService.sendCustomerOrderConfirmation(event.getOrderData());
            log.info("Customer order confirmation sent for order {}", event.getOrderData().orderId());
        } catch (Exception e) {
            log.error("Failed to send customer order confirmation for order {}",
                    event.getOrderData().orderId(), e);
        }
    }
}
