package com.quickbite.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentOrderRequest(
    @NotNull(message = "Order ID is required")
    Long orderId,

    @NotNull(message = "Customer ID is required")
    Long customerId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message = "Payment mode is required")
    String paymentMode,

    @NotBlank(message = "Currency is required")
    String currency
) {
}
