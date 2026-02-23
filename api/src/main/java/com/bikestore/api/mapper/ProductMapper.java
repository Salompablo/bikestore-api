package com.bikestore.api.mapper;

import com.bikestore.api.dto.request.ProductRequest;
import com.bikestore.api.dto.response.ProductResponse;
import com.bikestore.api.entity.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        return Product.builder()
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .category(request.category())
                .images(request.images() != null ? request.images() : new ArrayList<>())
                .isActive(true)
                .build();
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getImages(),
                product.getIsActive()
        );
    }

    public void updateProductFromRequest(Product target, ProductRequest source) {
        target.setName(source.name());
        target.setDescription(source.description());
        target.setPrice(source.price());
        target.setStock(source.stock());
        target.setCategory(source.category());
        target.setSku(source.sku());

        if (source.images() != null) {
            target.getImages().clear();
            target.getImages().addAll(source.images());
        }
    }
}
