package com.quickbite.review.service;

import java.util.List;

import com.quickbite.review.entity.Review;

public interface ReviewService {

    Review addReview(Review review);

    Review updateReview(Review review);

    void deleteReview(Long reviewId);

    List<Review> getReviewsByRestaurantId(Long restaurantId);

    List<Review> getReviewsByCustomerId(Long customerId);

    Review getReviewByOrderId(Long orderId);

    List<Review> getReviewsByAgentId(Long agentId);

    double getAverageFoodRating(Long restaurantId);

    double getAverageDeliveryRating(Long agentId);
}
