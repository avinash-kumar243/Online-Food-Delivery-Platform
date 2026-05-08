package com.quickbite.payment.dto;

import java.math.BigDecimal;

public record OrderSnapshotDto(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long deliveryAgentId,
    BigDecimal finalAmount
) {
}
