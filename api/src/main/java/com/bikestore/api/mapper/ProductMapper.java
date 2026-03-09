package com.bikestore.api.mapper;

import com.bikestore.api.dto.request.ProductRequest;
import com.bikestore.api.dto.response.CategoryResponse;
import com.bikestore.api.dto.response.ProductResponse;
import com.bikestore.api.entity.Category;
import com.bikestore.api.entity.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request, Category category) {
        return Product.builder()
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .category(category)
                .images(request.images() != null ? request.images() : new ArrayList<>())
                .isActive(true)
                .weight(request.weight())
                .length(request.length())
                .width(request.width())
                .height(request.height())
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
                new CategoryResponse(
                        product.getCategory().getId(),
                        product.getCategory().getName(),
                        product.getCategory().getDescription()
                ),
                product.getImages(),
                product.getIsActive(),
                product.getWeight(),
                product.getLength(),
                product.getWidth(),
                product.getHeight()
        );
    }

    public void updateProductFromRequest(Product target, ProductRequest source, Category newCategory) {
        target.setName(source.name());
        target.setDescription(source.description());
        target.setPrice(source.price());
        target.setStock(source.stock());
        target.setCategory(newCategory);
        target.setSku(source.sku());
        target.setWeight(source.weight());
        target.setLength(source.length());
        target.setWidth(source.width());
        target.setHeight(source.height());

        if (source.images() != null) {
            target.getImages().clear();
            target.getImages().addAll(source.images());
        }
    }
}
