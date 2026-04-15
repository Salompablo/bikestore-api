package com.bikestore.api.service.impl;

import com.bikestore.api.dto.request.ReviewRequest;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.dto.response.ReviewResponse;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.Review;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.Role;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.ReviewMapper;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.ReviewRepository;
import com.bikestore.api.repository.UserRepository;
import com.bikestore.api.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User user = getAuthenticatedUser();
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        reviewRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .ifPresent(existing -> {
                    throw new ConflictException("You already have a review for this product. Please edit your existing review instead.");
                });

        Review review = reviewMapper.toEntity(request, product, user);
        Review saved = reviewRepository.save(review);

        recalculateProductStats(product.getId());

        return reviewMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long id, ReviewRequest request) {
        User user = getAuthenticatedUser();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only edit your own reviews.");
        }

        review.setRating(request.rating());
        review.setComment(request.comment());
        Review updated = reviewRepository.save(review);

        recalculateProductStats(review.getProduct().getId());

        return reviewMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        User user = getAuthenticatedUser();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        boolean isOwner = review.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You can only delete your own reviews.");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);

        recalculateProductStats(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        Page<ReviewResponse> page = reviewRepository.findByProductId(productId, pageable)
                .map(reviewMapper::toResponse);

        return PageResponse.of(page);
    }

    private void recalculateProductStats(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        List<Review> reviews = reviewRepository.findByProductId(productId, Pageable.unpaged()).getContent();

        if (reviews.isEmpty()) {
            product.setAverageRating(0.0);
            product.setReviewCount(0);
        } else {
            double average = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            product.setAverageRating(Math.round(average * 10.0) / 10.0);
            product.setReviewCount(reviews.size());
        }

        productRepository.save(product);
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
