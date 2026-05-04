package com.quickbite.orderservice.client.dto;

public record DeliveryAgentRealtimeDto(
    Long agentId,
    Long userId,
    String fullName
) {
}
