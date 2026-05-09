package com.quickbite.notification.client.dto;

public record RestaurantResponseDto(
    Long restaurantId,
    Long ownerId,
    String name,
    Boolean isOpen,
    Boolean isApproved
) {
}
