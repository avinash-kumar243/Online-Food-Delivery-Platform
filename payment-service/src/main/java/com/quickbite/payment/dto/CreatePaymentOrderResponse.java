package com.quickbite.payment.dto;

public record CreatePaymentOrderResponse(
    Long paymentId,
    Long orderId,
    String razorpayOrderId,
    Long amount,
    String currency,
    String keyId
) {
}
