package com.quickbite.delivery.dto;

public record LocationUpdateResponse(
	Long agentId,
	double currentLatitude,
	double currentLongitude
) {
}
