package com.quickbite.restaurant.messaging.dto;

public record DeliveryEventDTO(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long agentId,
    String status,
    String location
) {
}
