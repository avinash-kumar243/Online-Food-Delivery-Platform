package com.quickbite.notification.client.dto;

public record DeliveryAgentResponseDto(
    Long agentId,
    Long userId,
    String fullName,
    boolean isAvailable,
    boolean isVerified,
    String verificationStatus
) {
}
