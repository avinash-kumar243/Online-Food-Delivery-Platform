package com.quickbite.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.review.dto.ReviewSubmissionRequest;
import com.quickbite.review.entity.Review;
import com.quickbite.review.entity.ReviewEligibility;
import com.quickbite.review.enums.ReviewType;
import com.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.review.repository.ReviewRepository;
import com.quickbite.review.service.impl.ReviewServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewAuthorizationValidator reviewAuthorizationValidator;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(reviewRepository, reviewAuthorizationValidator);
    }

    @Test
    void createFoodReview_CreatesNewReviewWithNormalizedComment() {
        ReviewSubmissionRequest request = new ReviewSubmissionRequest(10L, 20L, 5, "  Great food  ");
        ReviewEligibility eligibility = eligibility(10L, 20L, 30L, 40L);

        when(reviewAuthorizationValidator.validateDeliveredOrder(10L, 20L)).thenReturn(eligibility);
        when(reviewRepository.findByOrderIdAndCustomerIdAndReviewType(10L, 20L, ReviewType.FOOD)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setReviewId(99L);
            review.setReviewDate(LocalDateTime.now());
            return review;
        });

        var response = reviewService.createFoodReview(request);

        assertThat(response.reviewId()).isEqualTo(99L);
        assertThat(response.reviewType()).isEqualTo(ReviewType.FOOD);
        assertThat(response.comment()).isEqualTo("Great food");
        assertThat(response.restaurantId()).isEqualTo(30L);
        assertThat(response.agentId()).isEqualTo(40L);
    }

    @Test
    void createDeliveryReview_UpdatesExistingReview() {
        ReviewSubmissionRequest request = new ReviewSubmissionRequest(10L, 20L, 4, "  Fast delivery ");
        ReviewEligibility eligibility = eligibility(10L, 20L, 30L, 40L);
        Review existingReview = review(77L, 10L, 20L, 30L, 40L, ReviewType.DELIVERY, 2, "Old", LocalDateTime.now().minusDays(1));

        when(reviewAuthorizationValidator.validateDeliveredOrder(10L, 20L)).thenReturn(eligibility);
        when(reviewRepository.findByOrderIdAndCustomerIdAndReviewType(10L, 20L, ReviewType.DELIVERY))
            .thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(existingReview)).thenReturn(existingReview);

        var response = reviewService.createDeliveryReview(request);

        assertThat(response.reviewId()).isEqualTo(77L);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.comment()).isEqualTo("Fast delivery");
        verify(reviewRepository).save(existingReview);
    }

    @Test
    void getReviewById_WhenMissing_ThrowsReviewNotFoundException() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewById(404L))
            .isInstanceOf(ReviewNotFoundException.class)
            .hasMessage("Review not found with id: 404");
    }

    @Test
    void getReviewById_ReturnsMappedResponse() {
        Review review = review(1L, 10L, 20L, 30L, 40L, ReviewType.FOOD, 5, "Excellent", LocalDateTime.now());
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        var response = reviewService.getReviewById(1L);

        assertThat(response.reviewId()).isEqualTo(1L);
        assertThat(response.reviewType()).isEqualTo(ReviewType.FOOD);
        assertThat(response.comment()).isEqualTo("Excellent");
    }

    @Test
    void getReviewsAndAverages_ReturnExpectedValues() {
        Review newest = review(2L, 10L, 20L, 30L, 40L, ReviewType.FOOD, 4, "Good", LocalDateTime.now());
        Review oldest = review(1L, 11L, 21L, 31L, 41L, ReviewType.DELIVERY, 5, "Great", LocalDateTime.now().minusDays(1));

        when(reviewRepository.findByRestaurantIdAndReviewTypeOrderByReviewDateDesc(30L, ReviewType.FOOD)).thenReturn(List.of(newest));
        when(reviewRepository.findByCustomerIdOrderByReviewDateDesc(10L)).thenReturn(List.of(newest));
        when(reviewRepository.findByOrderIdOrderByReviewDateAsc(10L)).thenReturn(List.of(oldest, newest));
        when(reviewRepository.findByAgentIdAndReviewTypeOrderByReviewDateDesc(40L, ReviewType.DELIVERY)).thenReturn(List.of(oldest));
        when(reviewRepository.findAll()).thenReturn(List.of(oldest, newest));
        when(reviewRepository.findAverageFoodRatingByRestaurantId(30L)).thenReturn(4.5d);
        when(reviewRepository.findAverageDeliveryRatingByAgentId(40L)).thenReturn(null);

        assertThat(reviewService.getReviewsByRestaurantId(30L)).hasSize(1);
        assertThat(reviewService.getReviewsByCustomerId(10L)).hasSize(1);
        assertThat(reviewService.getReviewsByOrderId(10L)).extracting(r -> r.reviewId()).containsExactly(1L, 2L);
        assertThat(reviewService.getReviewsByAgentId(40L)).hasSize(1);
        assertThat(reviewService.getAllReviews()).extracting(r -> r.reviewId()).containsExactly(2L, 1L);
        assertThat(reviewService.getAverageFoodRating(30L)).isEqualTo(4.5d);
        assertThat(reviewService.getAverageDeliveryRating(40L)).isEqualTo(0.0d);
    }

    private ReviewEligibility eligibility(Long orderId, Long customerId, Long restaurantId, Long agentId) {
        return ReviewEligibility.builder()
            .eligibilityId(1L)
            .orderId(orderId)
            .customerId(customerId)
            .restaurantId(restaurantId)
            .agentId(agentId)
            .eligible(true)
            .enabledAt(LocalDateTime.now())
            .build();
    }

    private Review review(Long reviewId, Long orderId, Long customerId, Long restaurantId, Long agentId,
                          ReviewType reviewType, int rating, String comment, LocalDateTime reviewDate) {
        return Review.builder()
            .reviewId(reviewId)
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
