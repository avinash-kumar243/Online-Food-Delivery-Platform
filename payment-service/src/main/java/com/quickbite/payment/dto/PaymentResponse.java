package com.quickbite.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.quickbite.payment.enums.PaymentMode;
import com.quickbite.payment.enums.PaymentStatus;

public record PaymentResponse(
    Long paymentId,
    Long orderId,
    Long customerId,
    BigDecimal amount,
    PaymentStatus status,
    PaymentMode mode,
    String transactionId,
    String razorpayOrderId,
    String razorpayPaymentId,
    String currency,
    LocalDateTime paidAt,
    LocalDateTime refundedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
