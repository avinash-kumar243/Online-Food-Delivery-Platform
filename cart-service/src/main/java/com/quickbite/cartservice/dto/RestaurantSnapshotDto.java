package com.quickbite.cartservice.dto;

public record RestaurantSnapshotDto(
    Long restaurantId,
    Long ownerId,
    String name,
    Boolean isOpen,
    Boolean isApproved
) {
}
