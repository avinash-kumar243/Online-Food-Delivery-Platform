package com.quickbite.restaurant.messaging.dto;

public record DeliveryEventDTO(
    Long orderId,
    Long agentId,
    String status,
    String location
) {
}
