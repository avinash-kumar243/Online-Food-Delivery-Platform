package com.quickbite.cartservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AddCartItemRequest(
    @NotNull Long customerId,
    @NotNull Long restaurantId,
    @NotNull Long menuItemId,
    @NotBlank @Size(max = 120) String name,
    @NotNull @PositiveOrZero Double price,
    @NotNull @Positive Integer quantity,
    @Size(max = 255) String customization
) {
}
