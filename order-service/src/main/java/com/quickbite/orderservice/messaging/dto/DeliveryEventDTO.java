package com.quickbite.orderservice.messaging.dto;

public record DeliveryEventDTO(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long agentId,
    String status,
    String location
) {
}
