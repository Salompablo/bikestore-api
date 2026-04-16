package com.bikestore.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SendEmailEvent extends ApplicationEvent {

    private final String toEmail;
    private final String code;
    private final EmailType type;

    public SendEmailEvent(Object source, String toEmail, String code, EmailType type) {
        super(source);
        this.toEmail = toEmail;
        this.code = code;
        this.type = type;
    }

    public enum EmailType {
        VERIFICATION,
        REACTIVATION,
        PASSWORD_RESET
    }
}
