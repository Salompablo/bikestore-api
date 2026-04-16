package com.bikestore.api.repository;

import com.bikestore.api.entity.ShippingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingZoneRepository extends JpaRepository<ShippingZone, Long> {

    @Query("SELECT sz FROM ShippingZone sz ORDER BY LENGTH(sz.zipPrefix) DESC")
    List<ShippingZone> findAllOrderByZipPrefixLengthDesc();
}
