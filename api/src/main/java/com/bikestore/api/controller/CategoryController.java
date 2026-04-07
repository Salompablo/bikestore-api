package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiAdminErrors;
import com.bikestore.api.annotation.ApiNotFound;
import com.bikestore.api.dto.request.CategoryRequest;
import com.bikestore.api.dto.response.CategoryResponse;
import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints for managing product categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Get all categories", description = "Retrieves a list of all product categories (including inactive). Requires ADMIN privileges.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved categories")
    @ApiAdminErrors
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @Operation(summary = "Get active categories", description = "Retrieves a list of active product categories.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved active categories")
    @GetMapping("/active")
    public ResponseEntity<List<CategoryResponse>> getActiveCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    @Operation(summary = "Get category by ID", description = "Retrieves details of a specific category.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found"),
    })
    @ApiNotFound
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @Operation(summary = "Create a category", description = "Adds a new category to the catalog. Requires ADMIN privileges.")
    @ApiResponse(responseCode = "201", description = "Category successfully created")
    @ApiAdminErrors
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return new ResponseEntity<>(categoryService.createCategory(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a category", description = "Updates an existing category. Requires ADMIN privileges.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category successfully updated"),
    })
    @ApiAdminErrors
    @ApiNotFound
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @Operation(summary = "Delete a category", description = "Removes a category. Requires ADMIN privileges.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category successfully deleted")
    })
    @ApiAdminErrors
    @ApiNotFound
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}