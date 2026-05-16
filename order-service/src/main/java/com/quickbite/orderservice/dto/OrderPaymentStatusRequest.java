package com.quickbite.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderPaymentStatusRequest(@NotBlank String paymentStatus) {
}
