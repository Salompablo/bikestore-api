package com.bikestore.api.service.impl;

import com.bikestore.api.dto.request.CategoryRequest;
import com.bikestore.api.dto.response.CategoryResponse;
import com.bikestore.api.entity.Category;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.CategoryMapper;
import com.bikestore.api.repository.CategoryRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = getCategoryEntityById(id);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = getCategoryEntityById(id);

        categoryMapper.updateEntityFromRequest(request, category);

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryEntityById(id);

        if (productRepository.existsByCategoryIdAndIsActiveTrue(id)) {
            throw new ConflictException(
                    "Cannot deactivate category with id: " + id +
                    " because it has active products. Deactivate its products first.");
        }

        category.setIsActive(false);
        categoryRepository.save(category);
    }

    private Category getCategoryEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
}
