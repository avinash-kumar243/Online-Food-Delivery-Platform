package com.quickbite.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quickbite.review.entity.Review;
import com.quickbite.review.enums.ReviewType;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRestaurantIdAndReviewTypeOrderByReviewDateDesc(Long restaurantId, ReviewType reviewType);

    List<Review> findByCustomerIdOrderByReviewDateDesc(Long customerId);

    List<Review> findByOrderIdOrderByReviewDateAsc(Long orderId);

    List<Review> findByAgentIdAndReviewTypeOrderByReviewDateDesc(Long agentId, ReviewType reviewType);

    Optional<Review> findByOrderIdAndCustomerIdAndReviewType(Long orderId, Long customerId, ReviewType reviewType);

    @Query("select avg(r.rating) from Review r where r.restaurantId = :restaurantId and r.reviewType = com.quickbite.review.enums.ReviewType.FOOD")
    Double findAverageFoodRatingByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("select avg(r.rating) from Review r where r.agentId = :agentId and r.reviewType = com.quickbite.review.enums.ReviewType.DELIVERY")
    Double findAverageDeliveryRatingByAgentId(@Param("agentId") Long agentId);
}
