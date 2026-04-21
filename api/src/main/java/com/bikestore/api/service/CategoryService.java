package com.bikestore.api.service;

import com.bikestore.api.dto.request.CategoryRequest;
import com.bikestore.api.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    Page<CategoryResponse> getAllCategories(Pageable pageable);
    Page<CategoryResponse> getActiveCategories(Pageable pageable);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
    CategoryResponse activateCategory(Long id);
}
