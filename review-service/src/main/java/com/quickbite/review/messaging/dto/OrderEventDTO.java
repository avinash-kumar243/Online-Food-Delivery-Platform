package com.quickbite.review.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderEventDTO(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long deliveryAgentId,
    BigDecimal totalAmount,
    LocalDateTime timestamp
) {
}
