package com.quickbite.restaurant.dto;

import jakarta.validation.constraints.NotNull;

public record RestaurantApprovalRequest(
    @NotNull Boolean approved
) {
}
