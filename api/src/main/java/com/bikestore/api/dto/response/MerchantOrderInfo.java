package com.bikestore.api.dto.response;

import java.math.BigDecimal;

public record MerchantOrderInfo(
        String status,
        String orderStatus,
        String externalReference,
        BigDecimal paidAmount,
        BigDecimal totalAmount
) {
    public boolean isPaid() {
        if ("closed".equals(normalize(status)) || "paid".equals(normalize(orderStatus))) {
            return true;
        }
        return paidAmount != null
                && totalAmount != null
                && paidAmount.compareTo(totalAmount) >= 0;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase();
    }
}
