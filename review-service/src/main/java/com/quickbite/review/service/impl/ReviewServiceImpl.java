package com.quickbite.review.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.review.dto.ReviewResponse;
import com.quickbite.review.dto.ReviewSubmissionRequest;
import com.quickbite.review.entity.Review;
import com.quickbite.review.entity.ReviewEligibility;
import com.quickbite.review.enums.ReviewType;
import com.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.review.repository.ReviewRepository;
import com.quickbite.review.service.ReviewAuthorizationValidator;
import com.quickbite.review.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewAuthorizationValidator reviewAuthorizationValidator;

    @Override
    public ReviewResponse createFoodReview(ReviewSubmissionRequest request) {
        return createReview(request, ReviewType.FOOD);
    }

    @Override
    public ReviewResponse createDeliveryReview(ReviewSubmissionRequest request) {
        return createReview(request, ReviewType.DELIVERY);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long reviewId) {
        return toResponse(reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByRestaurantId(Long restaurantId) {
        return reviewRepository.findByRestaurantIdAndReviewTypeOrderByReviewDateDesc(restaurantId, ReviewType.FOOD).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByCustomerId(Long customerId) {
        return reviewRepository.findByCustomerIdOrderByReviewDateDesc(customerId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByOrderId(Long orderId) {
        return reviewRepository.findByOrderIdOrderByReviewDateAsc(orderId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByAgentId(Long agentId) {
        return reviewRepository.findByAgentIdAndReviewTypeOrderByReviewDateDesc(agentId, ReviewType.DELIVERY).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
            .sorted((left, right) -> right.getReviewDate().compareTo(left.getReviewDate()))
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageFoodRating(Long restaurantId) {
        Double average = reviewRepository.findAverageFoodRatingByRestaurantId(restaurantId);
        return average != null ? average : 0.0d;
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageDeliveryRating(Long agentId) {
        Double average = reviewRepository.findAverageDeliveryRatingByAgentId(agentId);
        return average != null ? average : 0.0d;
    }

    private ReviewResponse createReview(ReviewSubmissionRequest request, ReviewType reviewType) {
        ReviewEligibility eligibility = reviewAuthorizationValidator.validateDeliveredOrder(request.orderId(), request.customerId());
        String normalizedComment = normalizeComment(request.comment());

        Review review = reviewRepository.findByOrderIdAndCustomerIdAndReviewType(
                eligibility.getOrderId(),
                eligibility.getCustomerId(),
                reviewType
            )
            .map(existing -> updateExistingReview(existing, eligibility, request.rating(), normalizedComment))
            .orElseGet(() -> Review.builder()
                .orderId(eligibility.getOrderId())
                .customerId(eligibility.getCustomerId())
                .restaurantId(eligibility.getRestaurantId())
                .agentId(eligibility.getAgentId())
                .reviewType(reviewType)
                .rating(request.rating())
                .comment(normalizedComment)
                .verified(false)
                .build());

        review = reviewRepository.save(review);

        return toResponse(review);
    }

    private Review updateExistingReview(Review review, ReviewEligibility eligibility, int rating, String comment) {
        review.setRestaurantId(eligibility.getRestaurantId());
        review.setAgentId(eligibility.getAgentId());
        review.setRating(rating);
        review.setComment(comment);
        return review;
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
            review.getReviewId(),
            review.getOrderId(),
            review.getCustomerId(),
            review.getRestaurantId(),
            review.getAgentId(),
            review.getReviewType(),
            review.getRating(),
            review.getComment(),
            review.getReviewDate(),
            review.isVerified()
        );
    }
}
