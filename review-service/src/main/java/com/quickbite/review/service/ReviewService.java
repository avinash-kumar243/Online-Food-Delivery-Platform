package com.quickbite.review.service;

import java.util.List;

import com.quickbite.review.dto.ReviewResponse;
import com.quickbite.review.dto.ReviewSubmissionRequest;

public interface ReviewService {

    ReviewResponse createFoodReview(ReviewSubmissionRequest request);

    ReviewResponse createDeliveryReview(ReviewSubmissionRequest request);

    List<ReviewResponse> getReviewsByRestaurantId(Long restaurantId);

    List<ReviewResponse> getReviewsByCustomerId(Long customerId);

    List<ReviewResponse> getReviewsByOrderId(Long orderId);

    List<ReviewResponse> getReviewsByAgentId(Long agentId);

    List<ReviewResponse> getAllReviews();

    double getAverageFoodRating(Long restaurantId);

    double getAverageDeliveryRating(Long agentId);
}
