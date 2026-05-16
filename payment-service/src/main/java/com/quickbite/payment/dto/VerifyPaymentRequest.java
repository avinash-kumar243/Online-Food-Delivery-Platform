package com.quickbite.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyPaymentRequest(
    @NotNull(message = "Order ID is required")
    Long orderId,

    @NotBlank(message = "Razorpay order ID is required")
    String razorpayOrderId,

    @NotBlank(message = "Razorpay payment ID is required")
    String razorpayPaymentId,

    @NotBlank(message = "Razorpay signature is required")
    String razorpaySignature
) {
}
