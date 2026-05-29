package com.quickbite.menu.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleAvailabilityRequest(
    @NotNull Integer itemId,
    @NotNull Boolean available
) {
}
