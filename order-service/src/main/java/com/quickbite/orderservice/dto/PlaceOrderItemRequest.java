package com.quickbite.orderservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PlaceOrderItemRequest(
    @NotNull Long menuItemId,
    @NotBlank @Size(max = 120) String name,
    @NotNull @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @NotNull @Positive Integer quantity,
    @Size(max = 255) String customization
) {
}
