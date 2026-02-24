package com.bikestore.api.dto.response;

public record AuthResponse(
        String token,
        String message
) {
}
