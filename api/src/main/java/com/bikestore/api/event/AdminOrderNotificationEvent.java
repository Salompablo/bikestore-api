package com.bikestore.api.event;

import org.springframework.context.ApplicationEvent;

public class AdminOrderNotificationEvent extends ApplicationEvent {

    private final OrderPaidNotificationData orderData;

    public AdminOrderNotificationEvent(Object source, OrderPaidNotificationData orderData) {
        super(source);
        this.orderData = orderData;
    }

    public OrderPaidNotificationData getOrderData() {
        return orderData;
    }
}
