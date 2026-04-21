package com.bikestore.api.service;

import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.StockReservation;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.entity.enums.ReservationStatus;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    /**
     * Self-reference injected lazily to allow {@link #expireReservation(Long)} to be invoked
     * through the Spring proxy, enabling its {@code @Transactional} to take effect even when
     * called from within the same class (self-invocation).
     */
    @Lazy
    @Autowired
    private OrderCleanupService self;

    @Scheduled(fixedRate = 60_000)
    public void releaseExpiredReservations() {
        List<Long> expiredIds = stockReservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, LocalDateTime.now())
                .stream()
                .map(StockReservation::getId)
                .toList();

        if (expiredIds.isEmpty()) {
            return;
        }

        log.info("Found {} expired ACTIVE reservation(s). Processing...", expiredIds.size());

        for (Long reservationId : expiredIds) {
            try {
                self.expireReservation(reservationId);
            } catch (Exception e) {
                log.error("Failed to expire reservation id={}. Skipping.", reservationId, e);
            }
        }
    }

    @Transactional
    public void expireReservation(Long reservationId) {
        StockReservation reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        Long productId = reservation.getProduct().getId();
        Integer quantity = reservation.getQuantity();

        int updated = productRepository.releaseReservedStock(productId, quantity);
        if (updated == 0) {
            log.warn("Could not release reservedStock for product id={} (qty {}). Possible data inconsistency.",
                    productId, quantity);
        }

        reservation.setStatus(ReservationStatus.EXPIRED);

        Long orderId = reservation.getOrder().getId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.INITIATED || order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            log.info("Order {} cancelled due to expired reservation {}.", orderId, reservationId);
        }
    }
}
