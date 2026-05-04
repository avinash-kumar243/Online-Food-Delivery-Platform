package com.quickbite.review.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.review.dto.ReviewResponse;
import com.quickbite.review.dto.ReviewSubmissionRequest;
import com.quickbite.review.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/reviews", "/api/v1/reviews"})
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/food")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createFoodReview(@Valid @RequestBody ReviewSubmissionRequest request) {
        return reviewService.createFoodReview(request);
    }

    @PostMapping("/delivery")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createDeliveryReview(@Valid @RequestBody ReviewSubmissionRequest request) {
        return reviewService.createDeliveryReview(request);
    }

    @GetMapping("/restaurants/{restaurantId}")
    public List<ReviewResponse> getRestaurantReviews(@PathVariable Long restaurantId) {
        return reviewService.getReviewsByRestaurantId(restaurantId);
    }

    @GetMapping("/customers/{customerId}")
    public List<ReviewResponse> getCustomerReviews(@PathVariable Long customerId) {
        return reviewService.getReviewsByCustomerId(customerId);
    }

    @GetMapping("/orders/{orderId}")
    public List<ReviewResponse> getOrderReviews(@PathVariable Long orderId) {
        return reviewService.getReviewsByOrderId(orderId);
    }

    @GetMapping("/agents/{agentId}")
    public List<ReviewResponse> getDeliveryReviews(@PathVariable Long agentId) {
        return reviewService.getReviewsByAgentId(agentId);
    }

    @GetMapping("/admin")
    public List<ReviewResponse> getAllReviews() {
        return reviewService.getAllReviews();
    }

    @GetMapping("/restaurants/{restaurantId}/average")
    public double getAverageFoodRating(@PathVariable Long restaurantId) {
        return reviewService.getAverageFoodRating(restaurantId);
    }

    @GetMapping("/agents/{agentId}/average")
    public double getAverageDeliveryRating(@PathVariable Long agentId) {
        return reviewService.getAverageDeliveryRating(agentId);
    }
}
