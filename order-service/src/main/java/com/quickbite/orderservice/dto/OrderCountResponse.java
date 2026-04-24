package com.quickbite.orderservice.dto;

public record OrderCountResponse(
    Long restaurantId,
    long totalOrders
) {
}
