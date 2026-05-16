package com.quickbite.review.dto;

import java.time.LocalDateTime;

import com.quickbite.review.enums.ReviewType;

public record ReviewResponse(
    Long reviewId,
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long agentId,
    ReviewType reviewType,
    int rating,
    String comment,
    LocalDateTime reviewDate,
    boolean verified
) {
}
