package com.quickbite.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WalletTopUpRequest(
    @NotNull(message = "Customer ID is required")
    Long customerId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message = "Description is required")
    String description
) {
}
