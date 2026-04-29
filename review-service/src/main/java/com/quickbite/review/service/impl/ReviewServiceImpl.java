package com.quickbite.review.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.review.client.AgentClient;
import com.quickbite.review.client.CustomerClient;
import com.quickbite.review.client.OrderClient;
import com.quickbite.review.client.RestaurantClient;
import com.quickbite.review.client.dto.AgentDto;
import com.quickbite.review.client.dto.CustomerDto;
import com.quickbite.review.client.dto.OrderDto;
import com.quickbite.review.client.dto.RestaurantDto;
import com.quickbite.review.entity.Review;
import com.quickbite.review.exception.DuplicateReviewException;
import com.quickbite.review.exception.ResourceNotFoundException;
import com.quickbite.review.repository.ReviewRepository;
import com.quickbite.review.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderClient orderClient;
    private final CustomerClient customerClient;
    private final RestaurantClient restaurantClient;
    private final AgentClient agentClient;

    @Override
    public Review addReview(Review review) {
        if (reviewRepository.existsByOrderId(review.getOrderId())) {
            throw new DuplicateReviewException("A review already exists for orderId " + review.getOrderId());
        }

        validateReferences(review);
        if (review.getReviewDate() == null) {
            review.setReviewDate(LocalDate.now());
        }
        return reviewRepository.save(review);
    }

    @Override
    public Review updateReview(Review review) {
        if (review.getReviewId() == null) {
            throw new ResourceNotFoundException("reviewId is required for update");
        }

        Review existingReview = reviewRepository.findById(review.getReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for reviewId " + review.getReviewId()));

        if (!existingReview.getOrderId().equals(review.getOrderId())
                && reviewRepository.existsByOrderId(review.getOrderId())) {
            throw new DuplicateReviewException("A review already exists for orderId " + review.getOrderId());
        }

        validateReferences(review);

        existingReview.setOrderId(review.getOrderId());
        existingReview.setCustomerId(review.getCustomerId());
        existingReview.setRestaurantId(review.getRestaurantId());
        existingReview.setAgentId(review.getAgentId());
        existingReview.setFoodRating(review.getFoodRating());
        existingReview.setDeliveryRating(review.getDeliveryRating());
        existingReview.setComment(review.getComment());
        existingReview.setReviewDate(review.getReviewDate() != null ? review.getReviewDate() : existingReview.getReviewDate());
        existingReview.setVerified(review.isVerified());

        return reviewRepository.save(existingReview);
    }

    @Override
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for reviewId " + reviewId));
        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByRestaurantId(Long restaurantId) {
        return reviewRepository.findByRestaurantId(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByCustomerId(Long customerId) {
        return reviewRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Review getReviewByOrderId(Long orderId) {
        return reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for orderId " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByAgentId(Long agentId) {
        return reviewRepository.findByAgentId(agentId);
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

    private void validateReferences(Review review) {
        OrderDto order = orderClient.getOrderById(review.getOrderId());
        CustomerDto customer = customerClient.getCustomerById(review.getCustomerId());
        RestaurantDto restaurant = restaurantClient.getRestaurantById(review.getRestaurantId());
        AgentDto agent = agentClient.getAgentById(review.getAgentId());

        if (order == null) {
            throw new ResourceNotFoundException("Order not found for orderId " + review.getOrderId());
        }
        if (customer == null) {
            throw new ResourceNotFoundException("Customer not found for customerId " + review.getCustomerId());
        }
        if (restaurant == null) {
            throw new ResourceNotFoundException("Restaurant not found for restaurantId " + review.getRestaurantId());
        }
        if (agent == null) {
            throw new ResourceNotFoundException("Agent not found for agentId " + review.getAgentId());
        }

        if (order.getCustomerId() != null && !order.getCustomerId().equals(review.getCustomerId())) {
            throw new ResourceNotFoundException("orderId " + review.getOrderId() + " is not linked to customerId " + review.getCustomerId());
        }
        if (order.getRestaurantId() != null && !order.getRestaurantId().equals(review.getRestaurantId())) {
            throw new ResourceNotFoundException("orderId " + review.getOrderId() + " is not linked to restaurantId " + review.getRestaurantId());
        }
        if (order.getAgentId() != null && !order.getAgentId().equals(review.getAgentId())) {
            throw new ResourceNotFoundException("orderId " + review.getOrderId() + " is not linked to agentId " + review.getAgentId());
        }
    }
}
