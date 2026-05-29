package com.quickbite.delivery.dto;

public record AvailabilityUpdateResponse(
	Long agentId,
	boolean isAvailable,
	Long activeOrderId
) {
}
