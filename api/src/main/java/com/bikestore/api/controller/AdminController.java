package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiAdminErrors;
import com.bikestore.api.annotation.ApiNotFound;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.dto.response.UserResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.service.OrderService;
import com.bikestore.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Endpoints for platform administration (Users and Orders)")
public class AdminController {

    private final OrderService orderService;
    private final UserService userService;

    @Operation(summary = "Get all orders", description = "Retrieves a paginated list of all orders in the system.")
    @ApiResponse(responseCode = "200", description = "Orders successfully retrieved")
    @ApiAdminErrors
    @GetMapping("/orders")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<OrderResponse> springPage = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(PageResponse.of(springPage));
    }

    @Operation(summary = "Update order status", description = "Changes the status of a specific order (e.g., PENDING to SHIPPED).")
    @ApiResponse(responseCode = "200", description = "Order status successfully updated")
    @ApiNotFound
    @ApiAdminErrors
    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID", example = "1001") @PathVariable Long id,
            @Parameter(description = "New order status", example = "SHIPPED", required = true) @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @Operation(summary = "Get all users", description = "Retrieves a paginated list of all registered users.")
    @ApiResponse(responseCode = "200", description = "Users successfully retrieved")
    @ApiAdminErrors
    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "email", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<UserResponse> springPage = userService.getAllUsers(pageable);
        return ResponseEntity.ok(PageResponse.of(springPage));
    }
}