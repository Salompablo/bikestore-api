package com.bikestore.api.dto.response;

public record CategoryResponse(
        Long id,
        String name,
        String description
) {
}
