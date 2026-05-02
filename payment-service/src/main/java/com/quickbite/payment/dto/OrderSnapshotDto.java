package com.quickbite.payment.dto;

public record OrderSnapshotDto(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long deliveryAgentId
) {
}
