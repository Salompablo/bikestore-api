package com.bikestore.api.repository;

import com.bikestore.api.entity.StockReservation;
import com.bikestore.api.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderId(Long orderId);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime threshold);

    @Query("SELECT r FROM StockReservation r WHERE r.order.id = :orderId AND r.status = :status")
    List<StockReservation> findByOrderIdAndStatus(@Param("orderId") Long orderId,
                                                  @Param("status") ReservationStatus status);
}
