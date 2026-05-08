package com.quickbite.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.quickbite.review.entity.Review;
import com.quickbite.review.enums.ReviewType;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    private Review firstFoodReview;
    private Review secondFoodReview;
    private Review deliveryReview;

    @BeforeEach
    void setUp() {
        firstFoodReview = reviewRepository.save(review(10L, 20L, 30L, 40L, ReviewType.FOOD, 5, "Excellent", LocalDateTime.now().minusDays(2)));
        secondFoodReview = reviewRepository.save(review(10L, 21L, 30L, 41L, ReviewType.FOOD, 3, "Okay", LocalDateTime.now().minusDays(1)));
        deliveryReview = reviewRepository.save(review(11L, 20L, 31L, 40L, ReviewType.DELIVERY, 4, "Fast", LocalDateTime.now()));
    }

    @Test
    void queryMethods_ReturnExpectedReviewsAndAverages() {
        assertThat(reviewRepository.findByRestaurantIdAndReviewTypeOrderByReviewDateDesc(30L, ReviewType.FOOD))
            .extracting(Review::getReviewId)
            .containsExactly(secondFoodReview.getReviewId(), firstFoodReview.getReviewId());

        assertThat(reviewRepository.findByCustomerIdOrderByReviewDateDesc(20L))
            .extracting(Review::getReviewId)
            .containsExactly(deliveryReview.getReviewId(), firstFoodReview.getReviewId());

        assertThat(reviewRepository.findByOrderIdOrderByReviewDateAsc(10L))
            .extracting(Review::getReviewId)
            .containsExactly(firstFoodReview.getReviewId(), secondFoodReview.getReviewId());

        assertThat(reviewRepository.findByAgentIdAndReviewTypeOrderByReviewDateDesc(40L, ReviewType.DELIVERY))
            .extracting(Review::getReviewId)
            .containsExactly(deliveryReview.getReviewId());

        assertThat(reviewRepository.findByOrderIdAndCustomerIdAndReviewType(10L, 20L, ReviewType.FOOD))
            .contains(firstFoodReview);

        assertThat(reviewRepository.findAverageFoodRatingByRestaurantId(30L)).isEqualTo(4.0d);
        assertThat(reviewRepository.findAverageDeliveryRatingByAgentId(40L)).isEqualTo(4.0d);
    }

    private Review review(Long orderId, Long customerId, Long restaurantId, Long agentId, ReviewType reviewType,
                          int rating, String comment, LocalDateTime reviewDate) {
        return Review.builder()
            .orderId(orderId)
            .customerId(customerId)
            .restaurantId(restaurantId)
            .agentId(agentId)
            .reviewType(reviewType)
            .rating(rating)
            .comment(comment)
            .reviewDate(reviewDate)
            .verified(false)
            .build();
    }
}
