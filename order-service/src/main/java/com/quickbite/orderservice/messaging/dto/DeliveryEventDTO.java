package com.quickbite.orderservice.messaging.dto;

public record DeliveryEventDTO(
    Long orderId,
    Long agentId,
    String status,
    String location
) {
}
