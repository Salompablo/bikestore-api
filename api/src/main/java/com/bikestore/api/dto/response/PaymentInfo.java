package com.bikestore.api.dto.response;

public record PaymentInfo(
        String status,
        String externalReference
) {
}
