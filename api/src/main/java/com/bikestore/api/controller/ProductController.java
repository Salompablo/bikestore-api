package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiNotFound;
import com.bikestore.api.dto.request.ProductRequest;
import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.dto.response.ProductResponse;
import com.bikestore.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Endpoints for managing products and searching the public catalog")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get product catalog", description = "Retrieves a paginated list of active products. Supports optional filtering by category, name search, price range, and stock availability.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved catalog")
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getCatalog(
            @Parameter(description = "Optional category ID to filter by", example = "1")
            @RequestParam(required = false) Long categoryId,

            @Parameter(description = "Optional search query for product name", example = "Trek")
            @RequestParam(required = false) String search,

            @Parameter(description = "Optional minimum price filter", example = "100000")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Optional maximum price filter", example = "500000")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Optional filter for products in stock (stock > 0)", example = "true")
            @RequestParam(required = false) Boolean inStock,

            @Parameter(hidden = true)
            @PageableDefault(size = 12, sort = "name") Pageable pageable) {

        Page<ProductResponse> springPage = productService.getActiveProducts(categoryId, search, minPrice, maxPrice, inStock, pageable);

        return ResponseEntity.ok(PageResponse.of(springPage));
    }

    @Operation(summary = "Get all products (admin)", description = "Retrieves a paginated list of all products, including active and inactive ones. Requires ADMIN privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all products"),
            @ApiResponse(responseCode = "403", description = "Access denied (Not an Admin)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @Parameter(description = "Optional category ID to filter by", example = "1")
            @RequestParam(required = false) Long categoryId,

            @Parameter(description = "Optional search query for product name", example = "Trek")
            @RequestParam(required = false) String search,

            @Parameter(description = "Optional minimum price filter", example = "100000")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Optional maximum price filter", example = "500000")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Optional filter for products in stock (stock > 0)", example = "true")
            @RequestParam(required = false) Boolean inStock,

            @Parameter(hidden = true)
            @PageableDefault(size = 12, sort = "name") Pageable pageable) {

        Page<ProductResponse> springPage = productService.getAllProducts(categoryId, search, minPrice, maxPrice, inStock, pageable);

        return ResponseEntity.ok(PageResponse.of(springPage));
    }

    @Operation(summary = "Get product by ID", description = "Retrieves detailed information about a specific product.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
    })
    @ApiNotFound
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @Operation(summary = "Create a new product", description = "Adds a new product to the catalog. Requires ADMIN privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Not an Admin)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.create(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing product", description = "Updates a product's details. Requires ADMIN privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Not an Admin)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @ApiNotFound
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @Operation(summary = "Delete a product", description = "Removes a product from the catalog. Requires ADMIN privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied (Not an Admin)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @ApiNotFound
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activate a product", description = "Reactivates a previously deactivated product. Requires ADMIN privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product successfully activated"),
            @ApiResponse(responseCode = "403", description = "Access denied (Not an Admin)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Product is already active or its category is inactive", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @ApiNotFound
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.activateProduct(id));
    }
}
