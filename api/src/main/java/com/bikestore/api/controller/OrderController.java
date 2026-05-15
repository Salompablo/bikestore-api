package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiCustomerErrors;
import com.bikestore.api.annotation.ApiNotFound;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.service.OrderService;
import com.bikestore.api.util.SortResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for managing customer orders and purchase history")
public class OrderController {

    static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "id",
            "createdat", "createdAt",
            "updatedat", "updatedAt",
            "status", "status",
            "totalamount", "totalAmount"
    );
    static final String DEFAULT_SORT_FIELD = "createdAt";
    static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;

    private final OrderService orderService;

    @Operation(summary = "Get my orders", description = "Retrieves a paginated list of orders placed by the currently authenticated customer. Requires CUSTOMER privileges.")
    @ApiResponse(responseCode = "200", description = "Orders successfully retrieved")
    @ApiCustomerErrors
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders")
    public ResponseEntity<PageResponse<OrderResponse>> getMyOrders(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field. Allowed values: id, createdAt, updatedAt, status, totalAmount. Defaults to 'createdAt'.", example = "createdAt")
            @RequestParam(required = false) String sortField,

            @Parameter(description = "Sort direction: asc or desc. Defaults to 'desc'.", example = "desc")
            @RequestParam(required = false) String sortDirection,

            @Parameter(hidden = true) @AuthenticationPrincipal User authenticatedUser) {

        Pageable pageable = SortResolver.resolve(page, size, sortField, sortDirection,
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD, DEFAULT_SORT_DIRECTION);

        Page<OrderResponse> springPage = orderService.getMyOrders(authenticatedUser, pageable);
        return ResponseEntity.ok(PageResponse.of(springPage));
    }

    @Operation(summary = "Get my order by ID", description = "Retrieves the details of a specific order placed by the currently authenticated customer. Returns 404 if the order does not exist or does not belong to the user. Requires CUSTOMER privileges.")
    @ApiResponse(responseCode = "200", description = "Order successfully retrieved")
    @ApiNotFound
    @ApiCustomerErrors
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders/{id}")
    public ResponseEntity<OrderResponse> getMyOrderById(
            @Parameter(description = "Order ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User authenticatedUser) {

        return ResponseEntity.ok(orderService.getMyOrderById(id, authenticatedUser));
    }

    @Operation(summary = "Cancel an order", description = "Cancels an order that is still in INITIATED or PENDING status, releasing the stock reservations. Requires CUSTOMER privileges.")
    @ApiResponse(responseCode = "204", description = "Order successfully cancelled")
    @ApiCustomerErrors
    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @Parameter(description = "Order ID to cancel", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User authenticatedUser) {

        orderService.cancelOrder(id, authenticatedUser);
        return ResponseEntity.noContent().build();
    }
}