package com.quickbite.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quickbite.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRestaurantId(Long restaurantId);

    List<Review> findByCustomerId(Long customerId);

    Optional<Review> findByOrderId(Long orderId);

    List<Review> findByAgentId(Long agentId);

    boolean existsByOrderId(Long orderId);

    @Query("select avg(r.foodRating) from Review r where r.restaurantId = :restaurantId")
    Double findAverageFoodRatingByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("select avg(r.deliveryRating) from Review r where r.agentId = :agentId")
    Double findAverageDeliveryRatingByAgentId(@Param("agentId") Long agentId);
}
