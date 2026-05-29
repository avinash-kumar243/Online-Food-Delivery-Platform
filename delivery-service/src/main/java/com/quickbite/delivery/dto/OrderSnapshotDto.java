package com.quickbite.delivery.dto;

public record OrderSnapshotDto(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long deliveryAgentId
) {
}
