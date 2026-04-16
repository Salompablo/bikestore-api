package com.bikestore.api.service.impl;

import com.bikestore.api.dto.request.ProductRequest;
import com.bikestore.api.dto.response.ProductResponse;
import com.bikestore.api.entity.Category;
import com.bikestore.api.entity.Product;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.ProductMapper;
import com.bikestore.api.repository.CategoryRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.ReviewRepository;
import com.bikestore.api.service.ProductService;
import com.bikestore.api.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(Long categoryId, String search, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock, Pageable pageable) {
        Specification<Product> spec = ProductSpecification.buildCatalogSpec(categoryId, search, minPrice, maxPrice, inStock, true);
        return productRepository.findAll(spec, pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.findBySku(request.sku()).isPresent()) {
            throw new ConflictException("Product with SKU " + request.sku() + " already exists");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        validateCategoryIsActive(category);

        Product product = productMapper.toEntity(request, category);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!existingProduct.getSku().equals(request.sku()) && productRepository.findBySku(request.sku()).isPresent()) {
            throw new ConflictException("Product with SKU " + request.sku() + " already exists");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        validateCategoryIsActive(category);

        productMapper.updateProductFromRequest(existingProduct, request, category);
        return productMapper.toResponse(productRepository.save(existingProduct));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductResponse activateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (Boolean.TRUE.equals(product.getIsActive())) {
            throw new ConflictException("Product with id: " + id + " is already active.");
        }

        validateCategoryIsActive(product.getCategory());

        product.setIsActive(true);
        Product activatedProduct = productRepository.save(product);
        return productMapper.toResponse(activatedProduct);
    }

    @Override
    @Transactional
    public void recalculateProductStats(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Double average = reviewRepository.calculateAverageRatingByProductId(productId);
        Integer count = reviewRepository.countByProductId(productId);

        product.setAverageRating(Math.round(average * 10.0) / 10.0);
        product.setReviewCount(count);

        productRepository.save(product);
    }

    private void validateCategoryIsActive(Category category) {
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new ConflictException(
                    "Cannot assign product to inactive category with id: " + category.getId() +
                    ". Reactivate the category first or choose an active category.");
        }
    }
}
