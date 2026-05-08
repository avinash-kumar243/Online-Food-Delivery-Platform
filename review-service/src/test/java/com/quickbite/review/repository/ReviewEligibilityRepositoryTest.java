package com.quickbite.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.quickbite.review.entity.ReviewEligibility;

@DataJpaTest
class ReviewEligibilityRepositoryTest {

    @Autowired
    private ReviewEligibilityRepository reviewEligibilityRepository;

    @Test
    void findByOrderId_ReturnsSavedEligibility() {
        ReviewEligibility eligibility = reviewEligibilityRepository.save(ReviewEligibility.builder()
            .orderId(55L)
            .customerId(20L)
            .restaurantId(30L)
            .agentId(40L)
            .eligible(true)
            .enabledAt(LocalDateTime.now())
            .build());

        assertThat(reviewEligibilityRepository.findByOrderId(55L))
            .contains(eligibility);
    }
}
