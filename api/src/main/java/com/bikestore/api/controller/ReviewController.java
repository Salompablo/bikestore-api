package com.bikestore.api.controller;

import com.bikestore.api.dto.request.ReviewRequest;
import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.dto.response.ReviewResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Endpoints for managing product reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Get reviews for a product", description = "Retrieves a paginated list of reviews for a specific product. This endpoint is public.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved reviews"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/products/{productId}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByProduct(
            @PathVariable Long productId,
            @Parameter(hidden = true)
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }

    @Operation(summary = "Create a review", description = "Creates a new review for a product. Requires authentication. A user can only leave one review per product.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User already has a review for this product", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User authenticatedUser) {
        return new ResponseEntity<>(reviewService.createReview(request, authenticatedUser), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a review", description = "Updates an existing review. Only the review owner can update it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (not the review owner)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/v1/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User authenticatedUser) {
        return ResponseEntity.ok(reviewService.updateReview(id, request, authenticatedUser));
    }

    @Operation(summary = "Delete a review", description = "Deletes a review. Only the review owner or an ADMIN can delete it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Review successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/v1/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User authenticatedUser) {
        reviewService.deleteReview(id, authenticatedUser);
        return ResponseEntity.noContent().build();
    }
}
