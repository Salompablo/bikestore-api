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
public class OrderStatusUpdatedListener {

    private final EmailService emailService;

    @Async("adminEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderStatusUpdatedEvent event) {
        try {
            emailService.sendOrderStatusNotification(event.getData());
            log.info("Order status notification sent for order {} (status: {})",
                    event.getData().orderId(), event.getData().newStatus());
        } catch (Exception e) {
            log.error("Failed to send order status notification for order {} (status: {})",
                    event.getData().orderId(), event.getData().newStatus(), e);
        }
    }
}
