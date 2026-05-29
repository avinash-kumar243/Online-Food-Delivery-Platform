package com.quickbite.delivery.messaging.dto;

public record DeliveryEventDTO(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long restaurantOwnerId,
    Long agentId,
    Long deliveryPartnerUserId,
    String status,
    String location
) {
}
