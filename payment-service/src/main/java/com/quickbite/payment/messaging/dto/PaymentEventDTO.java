package com.quickbite.payment.messaging.dto;

import java.math.BigDecimal;

public record PaymentEventDTO(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long deliveryAgentId,
    String transactionId,
    String status,
    BigDecimal amount
) {
}
