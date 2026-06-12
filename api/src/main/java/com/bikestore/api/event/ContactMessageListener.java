package com.bikestore.api.event;

import com.bikestore.api.entity.ContactMessage;
import com.bikestore.api.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Asynchronous listener that sends an admin email notification
 * after a {@link ContactMessage} has been committed to the database.
 */
@Component
@Slf4j
public class ContactMessageListener {

    private final EmailService emailService;
    private final String adminEmail;

    public ContactMessageListener(
            EmailService emailService,
            @Value("${app.admin.email}") String adminEmail
    ) {
        this.emailService = emailService;
        this.adminEmail = adminEmail;
    }

    @Async("adminEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ContactMessageEvent event) {
        ContactMessage msg = event.getContactMessage();
        try {
            emailService.sendAdminContactNotification(adminEmail, msg);
            log.info("Admin contact notification sent for contact message id={}", msg.getId());
        } catch (Exception e) {
            log.error("Failed to send admin contact notification for message id={}", msg.getId(), e);
        }
    }
}
