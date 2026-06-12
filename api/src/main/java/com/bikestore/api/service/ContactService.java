package com.bikestore.api.service;

import com.bikestore.api.dto.request.ContactRequest;
import com.bikestore.api.entity.ContactMessage;
import com.bikestore.api.entity.enums.ContactStatus;
import com.bikestore.api.event.ContactMessageEvent;
import com.bikestore.api.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final CaptchaValidationService captchaValidationService;
    private final ContactMessageRepository contactMessageRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Validates the reCAPTCHA token, persists the contact message with PENDING status,
     * and publishes a {@link ContactMessageEvent} for asynchronous email notification.
     *
     * @param request the validated contact form payload
     */
    @Transactional
    public void processContactRequest(ContactRequest request) {
        captchaValidationService.validateToken(request.captchaToken());

        ContactMessage message = ContactMessage.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .orderId(request.orderId())
                .topic(request.topic())
                .message(request.message())
                .status(ContactStatus.PENDING)
                .build();

        ContactMessage saved = contactMessageRepository.save(message);
        log.info("Contact message saved with id={}", saved.getId());

        eventPublisher.publishEvent(new ContactMessageEvent(this, saved));
    }
}
