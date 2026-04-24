package com.quickbite.cartservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplyPromoRequest(
    @NotNull Long customerId,
    @NotBlank String promoCode
) {
}
