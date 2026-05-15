package com.bikestore.api.event;

import com.bikestore.api.dto.data.CustomerOrderConfirmationData;
import org.springframework.context.ApplicationEvent;

public class CustomerOrderConfirmationEvent extends ApplicationEvent {

    private final CustomerOrderConfirmationData orderData;

    public CustomerOrderConfirmationEvent(Object source, CustomerOrderConfirmationData orderData) {
        super(source);
        this.orderData = orderData;
    }

    public CustomerOrderConfirmationData getOrderData() {
        return orderData;
    }
}
