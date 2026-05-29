package com.quickbite.delivery.dto;

public record NearbyAgentResponse(
	Long agentId,
	Long userId,
	String fullName,
	String phone,
	String vehicleType,
	String vehicleNumber,
	double currentLatitude,
	double currentLongitude,
	boolean isAvailable,
	boolean isVerified,
	double avgRating,
	int totalDeliveries,
	Long activeOrderId,
	double distanceKm
) {
}
