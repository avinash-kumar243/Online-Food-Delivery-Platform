package com.quickbite.delivery.dto;

import java.time.LocalDateTime;

public record DeliveryAgentResponse(
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
	String verificationStatus,
	double avgRating,
	int totalDeliveries,
	Long activeOrderId,
	String rejectionReason,
	Long reviewedByAdminId,
	LocalDateTime reviewedAt,
	LocalDateTime submittedAt
) {
}
