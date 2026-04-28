package com.quickbite.delivery.dto;

import jakarta.validation.constraints.NotNull;

public record AvailabilityUpdateRequest(
	@NotNull(message = "Availability status is required")
	Boolean available
) {
}
