package com.bikestore.api.event;

import org.springframework.context.ApplicationEvent;

public class OrderStatusUpdatedEvent extends ApplicationEvent {

    private final OrderStatusUpdatedData data;

    public OrderStatusUpdatedEvent(Object source, OrderStatusUpdatedData data) {
        super(source);
        this.data = data;
    }

    public OrderStatusUpdatedData getData() {
        return data;
    }
}
