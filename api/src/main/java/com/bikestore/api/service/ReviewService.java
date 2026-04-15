package com.bikestore.api.service;

import com.bikestore.api.dto.request.ReviewRequest;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(ReviewRequest request);

    ReviewResponse updateReview(Long id, ReviewRequest request);

    void deleteReview(Long id);

    PageResponse<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable);
}
