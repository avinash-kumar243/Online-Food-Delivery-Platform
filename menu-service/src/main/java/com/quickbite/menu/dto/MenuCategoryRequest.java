package com.quickbite.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuCategoryRequest(
    Integer categoryId,
    @NotNull Integer restaurantId,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @Size(max = 500) String imageUrl,
    @NotNull @PositiveOrZero Integer displayOrder
) {
}
