package com.bikestore.api.specification;

import com.bikestore.api.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> nameContains(String search) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> hasStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stock"), 0);
    }

    public static Specification<Product> buildCatalogSpec(
            Long categoryId,
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            boolean activeOnly) {

        Specification<Product> spec = Specification.where((Specification<Product>) null);

        if (activeOnly) {
            spec = spec.and(isActive());
        }
        if (categoryId != null) {
            spec = spec.and(hasCategoryId(categoryId));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(nameContains(search));
        }
        if (minPrice != null) {
            spec = spec.and(priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(priceLessThanOrEqual(maxPrice));
        }
        if (Boolean.TRUE.equals(inStock)) {
            spec = spec.and(hasStock());
        }

        return spec;
    }
}
