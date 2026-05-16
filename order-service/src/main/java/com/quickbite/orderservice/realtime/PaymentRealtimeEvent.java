package com.quickbite.orderservice.realtime;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRealtimeEvent(
    RealtimeEventType type,
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long deliveryAgentId,
    String paymentStatus,
    BigDecimal amount,
    LocalDateTime occurredAt
) {
}
