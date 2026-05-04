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

@Component
@RequiredArgsConstructor
public class ReviewAuthorizationValidator {

    private final OrderClient orderClient;
    private final ReviewEligibilityRepository reviewEligibilityRepository;

    @Transactional
    public ReviewEligibility validateDeliveredOrder(Long orderId, Long customerId) {
        OrderDto order = orderClient.getOrderById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found for orderId " + orderId);
        }
        if (!"DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Reviews can only be submitted for delivered orders");
        }
        if (order.getCustomerId() == null || !order.getCustomerId().equals(customerId)) {
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
