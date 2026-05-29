package com.quickbite.cartservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCartItemQuantityRequest(
    @NotNull Long customerId,
    @NotNull Long itemId,
    @NotNull @Positive Integer quantity
) {
}
