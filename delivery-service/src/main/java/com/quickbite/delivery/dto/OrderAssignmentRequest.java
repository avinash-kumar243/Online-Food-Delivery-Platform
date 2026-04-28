package com.quickbite.delivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderAssignmentRequest(
	@NotNull(message = "Agent id is required")
	@Positive(message = "Agent id must be positive")
	Long agentId,

	@NotNull(message = "Order id is required")
	@Positive(message = "Order id must be positive")
	Long orderId
) {
}
