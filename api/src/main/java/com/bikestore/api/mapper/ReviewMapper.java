package com.bikestore.api.mapper;

import com.bikestore.api.dto.request.ReviewRequest;
import com.bikestore.api.dto.response.ReviewResponse;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.Review;
import com.bikestore.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public Review toEntity(ReviewRequest request, Product product, User user) {
        return Review.builder()
                .rating(request.rating())
                .comment(request.comment())
                .product(product)
                .user(user)
                .build();
    }

    public ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUser().getFirstName(),
                review.getUser().getLastName(),
                review.getUser().getEmail()
        );
    }
}
