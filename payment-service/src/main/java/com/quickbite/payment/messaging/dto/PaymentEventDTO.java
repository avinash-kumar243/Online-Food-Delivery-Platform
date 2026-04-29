package com.quickbite.payment.messaging.dto;

import java.math.BigDecimal;

public record PaymentEventDTO(
    Long orderId,
    String transactionId,
    String status,
    BigDecimal amount
) {
}
