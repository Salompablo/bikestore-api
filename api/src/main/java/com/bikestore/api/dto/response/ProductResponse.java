package com.bikestore.api.dto.response;

import com.bikestore.api.entity.enums.ProductCategory;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        ProductCategory category,
        List<String> images,
        Boolean isActive
) {
}
