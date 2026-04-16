package com.bikestore.api.service;

import com.bikestore.api.dto.request.ReviewRequest;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.dto.response.ReviewResponse;
import com.bikestore.api.entity.User;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(ReviewRequest request, User authenticatedUser);

    ReviewResponse updateReview(Long id, ReviewRequest request, User authenticatedUser);

    void deleteReview(Long id, User authenticatedUser);

    PageResponse<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable);
}
