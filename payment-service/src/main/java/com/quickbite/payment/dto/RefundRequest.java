package com.quickbite.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefundRequest(
    @NotNull(message = "Order ID is required")
    Long orderId,

    @NotBlank(message = "Refund reason is required")
    String reason
) {
}
