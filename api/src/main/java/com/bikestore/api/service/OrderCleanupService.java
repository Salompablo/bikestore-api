package com.bikestore.api.service;

import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.OrderItem;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(15);

        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, expirationThreshold);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} expired PENDING orders. Releasing inventory reservations...", expiredOrders.size());

        for (Order order : expiredOrders) {
            for (OrderItem item : order.getItems()) {
                productRepository.restoreStock(item.getProduct().getId(), item.getQuantity());
            }

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.info("Order {} cancelled. Inventory restored.", order.getId());
        }
    }
}
