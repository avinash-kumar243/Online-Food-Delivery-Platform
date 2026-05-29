package com.quickbite.review.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.review.client.OrderClient;
import com.quickbite.review.client.dto.OrderDto;
import com.quickbite.review.entity.ReviewEligibility;
import com.quickbite.review.exception.BadRequestException;
import com.quickbite.review.exception.ResourceNotFoundException;
import com.quickbite.review.repository.ReviewEligibilityRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewAuthorizationValidator {

    private final OrderClient orderClient;
    private final ReviewEligibilityRepository reviewEligibilityRepository;

    @Transactional
    public ReviewEligibility validateDeliveredOrder(Long orderId, Long customerId) {
        log.info("Validating review eligibility for orderId={} customerId={}", orderId, customerId);
        ReviewEligibility cachedEligibility = reviewEligibilityRepository.findByOrderId(orderId).orElse(null);
        if (cachedEligibility != null && cachedEligibility.isEligible()) {
            if (!cachedEligibility.getCustomerId().equals(customerId)) {
                log.warn("Review validation failed due to customer mismatch. orderId={} expectedCustomerId={} actualCustomerId={}",
                    orderId, cachedEligibility.getCustomerId(), customerId);
                throw new BadRequestException("This order is not associated with the provided customer");
            }
            return cachedEligibility;
        }

        OrderDto order = orderClient.getOrderById(orderId);
        if (order == null) {
            log.warn("Review validation failed because order was not found. orderId={}", orderId);
            throw new ResourceNotFoundException("Order not found for orderId " + orderId);
        }
        if (!"DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            log.warn("Review validation failed because order is not delivered. orderId={} status={}", orderId, order.getOrderStatus());
            throw new BadRequestException("Reviews can only be submitted for delivered orders");
        }
        if (order.getCustomerId() == null || !order.getCustomerId().equals(customerId)) {
            log.warn("Review validation failed because customerId does not match order. orderId={} expectedCustomerId={} actualCustomerId={}",
                orderId, order.getCustomerId(), customerId);
            throw new BadRequestException("This order is not associated with the provided customer");
        }
        if (order.getRestaurantId() == null) {
            throw new BadRequestException("Delivered order is missing restaurant information");
        }
        if (order.getAgentId() == null) {
            throw new BadRequestException("Delivered order is missing delivery partner information");
        }

        return reviewEligibilityRepository.findByOrderId(orderId)
            .map(existing -> refreshEligibility(existing, order))
            .orElseGet(() -> reviewEligibilityRepository.save(ReviewEligibility.builder()
                .orderId(orderId)
                .customerId(order.getCustomerId())
                .restaurantId(order.getRestaurantId())
                .agentId(order.getAgentId())
                .eligible(true)
                .enabledAt(LocalDateTime.now())
                .build()));
    }

    private ReviewEligibility refreshEligibility(ReviewEligibility existing, OrderDto order) {
        existing.setCustomerId(order.getCustomerId());
        existing.setRestaurantId(order.getRestaurantId());
        existing.setAgentId(order.getAgentId());
        existing.setEligible(true);
        if (existing.getEnabledAt() == null) {
            existing.setEnabledAt(LocalDateTime.now());
        }
        return reviewEligibilityRepository.save(existing);
    }
}
