package com.quickbite.orderservice.dto;

public record OrderRestaurantInfo(
    Long restaurantId,
    String name,
    String phone,
    String address,
    String city,
    Boolean isOpen,
    Boolean isApproved
) {
}
