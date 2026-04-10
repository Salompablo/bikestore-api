package com.bikestore.api.service;

import com.bikestore.api.dto.request.ProductRequest;
import com.bikestore.api.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {
    Page<ProductResponse> getActiveProducts(Long categoryId, String search, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock, Pageable pageable);
    Page<ProductResponse> getAllProducts(Long categoryId, String search, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock, Pageable pageable);
    ProductResponse getById(Long id);
    ProductResponse create(ProductRequest request);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
    ProductResponse activateProduct(Long id);
}
