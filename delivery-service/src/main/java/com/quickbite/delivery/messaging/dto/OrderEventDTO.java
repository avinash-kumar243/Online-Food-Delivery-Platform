package com.quickbite.delivery.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderEventDTO(
    Long orderId,
    Long customerId,
    Long restaurantId,
    BigDecimal totalAmount,
    LocalDateTime timestamp
) {
}
