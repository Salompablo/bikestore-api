package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiAdminErrors;
import com.bikestore.api.annotation.ApiNotFound;
import com.bikestore.api.dto.request.ShippingZoneRequest;
import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.dto.response.ShippingZoneResponse;
import com.bikestore.api.dto.response.UserResponse;
import com.bikestore.api.entity.ShippingZone;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.ShippingZoneMapper;
import com.bikestore.api.repository.ShippingZoneRepository;
import com.bikestore.api.service.OrderService;
import com.bikestore.api.service.UserService;
import com.bikestore.api.util.SortResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Endpoints for platform administration (Users, Orders, and Shipping Zones)")
public class AdminController {

    static final Map<String, String> ALLOWED_ORDER_SORT_FIELDS = Map.of(
            "id", "id",
            "createdat", "createdAt",
            "updatedat", "updatedAt",
            "status", "status",
            "totalamount", "totalAmount"
    );
    static final String DEFAULT_ORDER_SORT_FIELD = "createdAt";
    static final Sort.Direction DEFAULT_ORDER_SORT_DIRECTION = Sort.Direction.DESC;

    static final Map<String, String> ALLOWED_USER_SORT_FIELDS = Map.of(
            "id", "id",
            "email", "email",
            "firstname", "firstName",
            "lastname", "lastName"
    );
    static final String DEFAULT_USER_SORT_FIELD = "email";
    static final Sort.Direction DEFAULT_USER_SORT_DIRECTION = Sort.Direction.ASC;

    private final OrderService orderService;
    private final UserService userService;
    private final ShippingZoneRepository shippingZoneRepository;
    private final ShippingZoneMapper shippingZoneMapper;

    @Operation(summary = "Get all orders", description = "Retrieves a paginated list of all orders in the system.")
    @ApiResponse(responseCode = "200", description = "Orders successfully retrieved")
    @ApiAdminErrors
    @GetMapping("/orders")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field. Allowed values: id, createdAt, updatedAt, status, totalAmount. Defaults to 'createdAt'.", example = "createdAt")
            @RequestParam(required = false) String sortField,

            @Parameter(description = "Sort direction: asc or desc. Defaults to 'desc'.", example = "desc")
            @RequestParam(required = false) String sortDirection) {

        Pageable pageable = SortResolver.resolve(page, size, sortField, sortDirection,
                ALLOWED_ORDER_SORT_FIELDS, DEFAULT_ORDER_SORT_FIELD, DEFAULT_ORDER_SORT_DIRECTION);

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
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field. Allowed values: id, email, firstName, lastName. Defaults to 'email'.", example = "email")
            @RequestParam(required = false) String sortField,

            @Parameter(description = "Sort direction: asc or desc. Defaults to 'asc'.", example = "asc")
            @RequestParam(required = false) String sortDirection) {

        Pageable pageable = SortResolver.resolve(page, size, sortField, sortDirection,
                ALLOWED_USER_SORT_FIELDS, DEFAULT_USER_SORT_FIELD, DEFAULT_USER_SORT_DIRECTION);

        Page<UserResponse> springPage = userService.getAllUsers(pageable);
        return ResponseEntity.ok(PageResponse.of(springPage));
    }

    // ── Shipping Zone Management ──

    @Operation(summary = "List all shipping zones", description = "Retrieves all configured shipping zones ordered by ZIP prefix specificity.")
    @ApiResponse(responseCode = "200", description = "Shipping zones retrieved successfully")
    @ApiAdminErrors
    @GetMapping("/shipping-zones")
    public ResponseEntity<List<ShippingZoneResponse>> getAllShippingZones() {
        List<ShippingZoneResponse> zones = shippingZoneRepository.findAllOrderByZipPrefixLengthDesc()
                .stream()
                .map(shippingZoneMapper::toResponse)
                .toList();
        return ResponseEntity.ok(zones);
    }

    @Operation(summary = "Create a shipping zone", description = "Adds a new shipping zone. The store owner defines the ZIP prefix, cost, and estimated delivery days.")
    @ApiResponse(responseCode = "201", description = "Shipping zone created successfully")
    @ApiAdminErrors
    @PostMapping("/shipping-zones")
    public ResponseEntity<ShippingZoneResponse> createShippingZone(@Valid @RequestBody ShippingZoneRequest request) {
        ShippingZone zone = shippingZoneMapper.toEntity(request);
        ShippingZone saved = shippingZoneRepository.save(zone);
        return new ResponseEntity<>(shippingZoneMapper.toResponse(saved), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a shipping zone", description = "Updates an existing shipping zone's configuration.")
    @ApiResponse(responseCode = "200", description = "Shipping zone updated successfully")
    @ApiNotFound
    @ApiAdminErrors
    @PutMapping("/shipping-zones/{id}")
    public ResponseEntity<ShippingZoneResponse> updateShippingZone(
            @PathVariable Long id,
            @Valid @RequestBody ShippingZoneRequest request) {
        ShippingZone zone = shippingZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping zone not found with id: " + id));
        shippingZoneMapper.updateFromRequest(zone, request);
        ShippingZone updated = shippingZoneRepository.save(zone);
        return ResponseEntity.ok(shippingZoneMapper.toResponse(updated));
    }

    @Operation(summary = "Delete a shipping zone", description = "Removes a shipping zone from the system.")
    @ApiResponse(responseCode = "204", description = "Shipping zone deleted successfully")
    @ApiNotFound
    @ApiAdminErrors
    @DeleteMapping("/shipping-zones/{id}")
    public ResponseEntity<Void> deleteShippingZone(@PathVariable Long id) {
        ShippingZone zone = shippingZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping zone not found with id: " + id));
        shippingZoneRepository.delete(zone);
        return ResponseEntity.noContent().build();
    }
}