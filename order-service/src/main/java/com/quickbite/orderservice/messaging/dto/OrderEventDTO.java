package com.quickbite.orderservice.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderEventDTO(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long restaurantOwnerId,
    Long deliveryAgentId,
    BigDecimal totalAmount,
    LocalDateTime timestamp
) {
}
