package com.quickbite.review.messaging.dto;

public record ReviewNotificationEventDTO(
    Long reviewId,
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long agentId,
    String reviewType,
    int rating,
    String comment
) {
}
