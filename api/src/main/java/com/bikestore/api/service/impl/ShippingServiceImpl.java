package com.bikestore.api.service.impl;

import com.bikestore.api.dto.response.ShippingQuoteResponse;
import com.bikestore.api.entity.ShippingZone;
import com.bikestore.api.repository.ShippingZoneRepository;
import com.bikestore.api.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShippingZoneRepository shippingZoneRepository;

    @Override
    @Transactional(readOnly = true)
    public ShippingQuoteResponse calculateShippingCost(String zipCode, Double totalWeight) {

        List<ShippingZone> zones = shippingZoneRepository.findAllOrderByZipPrefixLengthDesc();

        for (ShippingZone zone : zones) {
            if (zipCode.startsWith(zone.getZipPrefix())) {
                return new ShippingQuoteResponse(zone.getProvider(), zone.getCost(), zone.getEstimatedDays());
            }
        }

        // Default fallback when no zone matches
        return new ShippingQuoteResponse(
                "Envío gestionado por Bikes Asaro",
                new BigDecimal("25000.00"),
                7
        );
    }
}
