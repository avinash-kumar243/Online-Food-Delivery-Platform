package com.quickbite.menu.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuItemRequest(
    Integer itemId,
    @NotNull Integer restaurantId,
    @NotNull Integer categoryId,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @NotNull @Positive Double price,
    @Positive Double discountedPrice,
    @Size(max = 500) String imageUrl,
    @NotNull Boolean isVeg,
    @NotNull Boolean isAvailable,
    @DecimalMin("0.0") @DecimalMax("5.0") Double rating,
    @PositiveOrZero Integer calories,
    @Size(max = 255) String tags
) {
}
