package com.bikestore.api.event;

import com.bikestore.api.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendEmailEventListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSendEmailEvent(SendEmailEvent event) {
        log.info("Sending {} email to {} after transaction commit", event.getType(), event.getToEmail());

        switch (event.getType()) {
            case VERIFICATION -> emailService.sendVerificationEmail(event.getToEmail(), event.getCode());
            case REACTIVATION -> emailService.sendReactivationEmail(event.getToEmail(), event.getCode());
            case PASSWORD_RESET -> emailService.sendPasswordResetEmail(event.getToEmail(), event.getCode());
        }
    }
}
