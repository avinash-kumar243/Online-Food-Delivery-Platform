package com.quickbite.menu.dto;

import jakarta.validation.constraints.NotNull;

public record MenuDeleteRequest(
    @NotNull MenuEntityType type,
    Integer itemId,
    Integer categoryId
) {
}
