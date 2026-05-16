package com.quickbite.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record MenuMutationRequest(
    @NotNull MenuEntityType type,
    @Valid MenuItemRequest item,
    @Valid MenuCategoryRequest category
) {
}
