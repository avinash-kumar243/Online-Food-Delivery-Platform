package com.quickbite.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRestaurantDecisionRequest(
    @NotNull Long adminId,
    @Size(max = 500) String feedback
) {
}
