package com.bikestore.api.event;

import org.springframework.context.ApplicationEvent;

public class ShippingQuoteRequestedEvent extends ApplicationEvent {

    private final ShippingQuoteRequestedData data;

    public ShippingQuoteRequestedEvent(Object source, ShippingQuoteRequestedData data) {
        super(source);
        this.data = data;
    }

    public ShippingQuoteRequestedData getData() {
        return data;
    }
}
