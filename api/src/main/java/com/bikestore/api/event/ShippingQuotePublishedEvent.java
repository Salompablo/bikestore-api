package com.bikestore.api.event;

import org.springframework.context.ApplicationEvent;

public class ShippingQuotePublishedEvent extends ApplicationEvent {

    private final ShippingQuotePublishedData data;

    public ShippingQuotePublishedEvent(Object source, ShippingQuotePublishedData data) {
        super(source);
        this.data = data;
    }

    public ShippingQuotePublishedData getData() {
        return data;
    }
}
