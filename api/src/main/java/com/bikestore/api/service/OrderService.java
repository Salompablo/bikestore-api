package com.bikestore.api.service;

import com.bikestore.api.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    Page<OrderResponse> getMyOrders(Pageable pageable);
}
