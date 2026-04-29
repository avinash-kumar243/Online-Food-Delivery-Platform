package com.quickbite.review.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.review.entity.ReviewEligibility;

public interface ReviewEligibilityRepository extends JpaRepository<ReviewEligibility, Long> {

    Optional<ReviewEligibility> findByOrderId(Long orderId);
}
