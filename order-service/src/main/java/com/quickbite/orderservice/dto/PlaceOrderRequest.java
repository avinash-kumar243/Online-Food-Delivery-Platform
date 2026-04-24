package com.quickbite.orderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
    @NotNull Long customerId,
    @NotNull Long restaurantId,
    @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal discount,
    @NotBlank @Size(max = 40) String modeOfPayment,
    @FutureOrPresent LocalDateTime estimatedDelivery,
    @NotBlank @Size(max = 500) String deliveryAddress,
    @Size(max = 500) String specialInstructions,
    @NotEmpty @Valid List<PlaceOrderItemRequest> items
) {
}
