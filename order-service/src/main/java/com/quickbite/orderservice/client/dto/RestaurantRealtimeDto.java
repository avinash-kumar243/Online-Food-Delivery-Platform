package com.quickbite.orderservice.client.dto;

public record RestaurantRealtimeDto(
    Long restaurantId,
    Long ownerId,
    String name,
    String phone,
    String address,
    String city,
    Boolean isOpen,
    Boolean isApproved
) {
}
