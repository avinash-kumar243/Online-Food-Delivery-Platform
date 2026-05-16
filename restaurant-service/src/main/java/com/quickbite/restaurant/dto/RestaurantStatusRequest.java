package com.quickbite.restaurant.dto;

import jakarta.validation.constraints.NotNull;

public record RestaurantStatusRequest(
    @NotNull Boolean open
) {
}
