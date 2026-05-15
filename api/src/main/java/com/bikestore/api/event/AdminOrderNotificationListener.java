package com.bikestore.api.event;

import com.bikestore.api.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminOrderNotificationListener {

    private final EmailService emailService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Async("adminEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAdminOrderNotification(AdminOrderNotificationEvent event) {
        try {
            emailService.sendAdminOrderNotification(adminEmail, event.getOrderData());
            log.info("Admin paid-order notification sent for order {}", event.getOrderData().orderId());
        } catch (Exception e) {
            log.error("Failed to send admin paid-order notification for order {}",
                    event.getOrderData().orderId(), e);
        }
    }
}
