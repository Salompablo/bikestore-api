package com.bikestore.api.mapper;

import com.bikestore.api.dto.request.ShippingZoneRequest;
import com.bikestore.api.dto.response.ShippingZoneResponse;
import com.bikestore.api.entity.ShippingZone;
import org.springframework.stereotype.Component;

@Component
public class ShippingZoneMapper {

    public ShippingZone toEntity(ShippingZoneRequest request) {
        return ShippingZone.builder()
                .name(request.name())
                .zipPrefix(request.zipPrefix())
                .cost(request.cost())
                .estimatedDays(request.estimatedDays())
                .provider(request.provider() != null ? request.provider() : "Envío gestionado por Bikes Asaro")
                .build();
    }

    public ShippingZoneResponse toResponse(ShippingZone zone) {
        return new ShippingZoneResponse(
                zone.getId(),
                zone.getName(),
                zone.getZipPrefix(),
                zone.getCost(),
                zone.getEstimatedDays(),
                zone.getProvider()
        );
    }

    public void updateFromRequest(ShippingZone zone, ShippingZoneRequest request) {
        zone.setName(request.name());
        zone.setZipPrefix(request.zipPrefix());
        zone.setCost(request.cost());
        zone.setEstimatedDays(request.estimatedDays());
        if (request.provider() != null) {
            zone.setProvider(request.provider());
        }
    }
}
