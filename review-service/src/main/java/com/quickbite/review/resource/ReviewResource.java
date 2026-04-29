package com.quickbite.review.resource;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.review.entity.Review;
import com.quickbite.review.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewResource {

    private final ReviewService reviewService;

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public Review addReview(@Valid @RequestBody Review review) {
        return reviewService.addReview(review);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<Review> getReviewsByRestaurant(@PathVariable Long restaurantId) {
        return reviewService.getReviewsByRestaurantId(restaurantId);
    }

    @GetMapping("/customer/{customerId}")
    public List<Review> getReviewsByCustomer(@PathVariable Long customerId) {
        return reviewService.getReviewsByCustomerId(customerId);
    }

    @GetMapping("/order/{orderId}")
    public Review getReviewByOrder(@PathVariable Long orderId) {
        return reviewService.getReviewByOrderId(orderId);
    }

    @GetMapping("/agent/{agentId}")
    public List<Review> getReviewsByAgent(@PathVariable Long agentId) {
        return reviewService.getReviewsByAgentId(agentId);
    }

    @GetMapping("/avgFood/{restaurantId}")
    public double getAverageFoodRating(@PathVariable Long restaurantId) {
        return reviewService.getAverageFoodRating(restaurantId);
    }

    @GetMapping("/avgDelivery/{agentId}")
    public double getAverageDeliveryRating(@PathVariable Long agentId) {
        return reviewService.getAverageDeliveryRating(agentId);
    }

    @PutMapping("/update")
    public Review updateReview(@Valid @RequestBody Review review) {
        return reviewService.updateReview(review);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable("id") Long reviewId) {
        reviewService.deleteReview(reviewId);
    }
}
