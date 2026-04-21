package com.bikestore.api.service;

import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.StockReservation;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.entity.enums.ReservationStatus;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.StockReservationRepository;
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
    private final StockReservationRepository stockReservationRepository;

    @Scheduled(fixedRate = 60_000)
    public void releaseExpiredReservations() {
        List<StockReservation> expired = stockReservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, LocalDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        log.info("Found {} expired ACTIVE reservation(s). Processing...", expired.size());

        for (StockReservation reservation : expired) {
            try {
                expireReservation(reservation);
            } catch (Exception e) {
                log.error("Failed to expire reservation id={} for order id={}. Skipping.",
                        reservation.getId(), reservation.getOrder().getId(), e);
            }
        }
    }

    @Transactional
    public void expireReservation(StockReservation reservation) {
        Long productId = reservation.getProduct().getId();
        Integer quantity = reservation.getQuantity();

        int updated = productRepository.releaseReservedStock(productId, quantity);
        if (updated == 0) {
            log.warn("Could not release reservedStock for product id={} (qty {}). Possible data inconsistency.",
                    productId, quantity);
        }

        reservation.setStatus(ReservationStatus.EXPIRED);
        stockReservationRepository.save(reservation);

        Order order = reservation.getOrder();
        if (order.getStatus() == OrderStatus.INITIATED || order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Order {} cancelled due to expired reservation {}.", order.getId(), reservation.getId());
        }
    }
}
