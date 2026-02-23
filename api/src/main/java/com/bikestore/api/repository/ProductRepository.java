package com.bikestore.api.repository;

import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategory(ProductCategory category, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByIsActiveTrue(Pageable pageable);
    Optional<Product> findBySku(String sku);
}
