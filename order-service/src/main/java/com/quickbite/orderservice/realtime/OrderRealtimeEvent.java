package com.quickbite.orderservice.realtime;

import java.time.LocalDateTime;

public record OrderRealtimeEvent(
    RealtimeEventType type,
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long deliveryAgentId,
    String orderStatus,
    String paymentStatus,
    LocalDateTime occurredAt
) {
}
