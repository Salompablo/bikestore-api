package com.bikestore.api.service.impl;

import com.bikestore.api.dto.request.ProductRequest;
import com.bikestore.api.dto.response.ProductResponse;
import com.bikestore.api.entity.Product;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.ProductMapper;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable).map(productMapper::toResponse);
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
        Product product = productMapper.toEntity(request);
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

        productMapper.updateProductFromRequest(existingProduct, request);
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
}
