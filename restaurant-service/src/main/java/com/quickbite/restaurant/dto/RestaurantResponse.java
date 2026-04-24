package com.quickbite.restaurant.dto;

public record RestaurantResponse(
    Long restaurantId,
    Long ownerId,
    String name,
    String description,
    String cuisine,
    String address,
    String city,
    Double latitude,
    Double longitude,
    String phone,
    Double avgRating,
    Double deliveryRadius,
    Boolean isOpen,
    Boolean isApproved,
    Integer minOrderAmount,
    Integer estimatedDeliveryMin
) {
}
