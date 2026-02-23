package com.bikestore.api.service;

import com.bikestore.api.dto.request.ProductRequest;
import com.bikestore.api.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Page<ProductResponse> getAll(Pageable pageable);
    ProductResponse getById(Long id);
    ProductResponse create(ProductRequest request);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
}
