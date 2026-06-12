package com.bikestore.api.event;

import com.bikestore.api.entity.ContactMessage;
import org.springframework.context.ApplicationEvent;

/**
 * Application event fired after a {@link ContactMessage} is persisted.
 * A listener will handle asynchronous email notification.
 */
public class ContactMessageEvent extends ApplicationEvent {

    private final ContactMessage contactMessage;

    public ContactMessageEvent(Object source, ContactMessage contactMessage) {
        super(source);
        this.contactMessage = contactMessage;
    }

    public ContactMessage getContactMessage() {
        return contactMessage;
    }
}
