package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiCustomerErrors;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for managing customer orders and purchase history")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Get my orders", description = "Retrieves a paginated list of orders placed by the currently authenticated customer. Requires CUSTOMER privileges.")
    @ApiResponse(responseCode = "200", description = "Orders successfully retrieved")
    @ApiCustomerErrors
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders")
    public ResponseEntity<PageResponse<OrderResponse>> getMyOrders(
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal User authenticatedUser) {

        Page<OrderResponse> springPage = orderService.getMyOrders(authenticatedUser, pageable);
        return ResponseEntity.ok(PageResponse.of(springPage));
    }
}