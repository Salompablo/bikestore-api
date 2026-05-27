package com.bikestore.api.service;

import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class OrderStatusTransitionPolicy {

    public void validateTransition(Order order, OrderStatus newStatus) {
        OrderStatus current = order.getStatus();
        DeliveryMethod method = order.getDeliveryMethod();

        if (newStatus == OrderStatus.CANCELLED) {
            return;
        }

        Set<OrderStatus> allowed;

        if (method == DeliveryMethod.STORE_PICKUP) {
            allowed = switch (current) {
                case PAID -> EnumSet.of(OrderStatus.READY_FOR_PICKUP);
                case READY_FOR_PICKUP -> EnumSet.of(OrderStatus.PICKED_UP);
                default -> EnumSet.noneOf(OrderStatus.class);
            };
        } else {
            allowed = switch (current) {
                case PAID -> EnumSet.of(OrderStatus.SHIPPED);
                case SHIPPED -> EnumSet.of(OrderStatus.DELIVERED);
                default -> EnumSet.noneOf(OrderStatus.class);
            };
        }

        if (!allowed.contains(newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Cannot transition order %d from %s to %s (delivery method: %s)",
                            order.getId(), current, newStatus, method));
        }
    }
}
