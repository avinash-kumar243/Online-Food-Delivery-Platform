package com.quickbite.restaurant.messaging.dto;

public record RestaurantEventDTO(
    Long restaurantId,
    Long ownerId,
    String name,
    Boolean isOpen,
    Boolean isApproved,
    String approvalStatus,
    String reason
) {
}
